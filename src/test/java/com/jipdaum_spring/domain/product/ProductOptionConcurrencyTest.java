package com.jipdaum_spring.domain.product;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderService.verifyPayment이 재고 차감에 쓰는 ProductOptionRepository.decrementStockIfAvailable
 * (DB 레벨 조건부 UPDATE)이 실제 동시 요청 아래서도 재고를 음수로 만들지 않는지 검증한다.
 *
 * 재고 5개짜리 옵션에 20개 스레드를 동시에 출발시켜 각자 수량 1씩 차감을 시도한다. 스레드마다
 * 별도 트랜잭션(TransactionTemplate)으로 실행해 실제 동시 HTTP 요청과 같은 조건(서로 다른
 * DB 커넥션·트랜잭션)을 만든다 — 테스트 메서드 전체를 @Transactional로 감싸면 한 트랜잭션
 * 안에서 순차 실행되는 것과 다를 게 없어 의미가 없다.
 *
 * 공유 개발 DB(Django와 같은 인스턴스)에 실제 행을 만들고 지우므로 @AfterEach로 반드시 정리한다.
 *
 * 참고: WHERE 가드를 일부러 지우고 돌려본 결과, stock_count 컬럼이 DB 스키마상 BIGINT UNSIGNED라
 * 음수가 되면 MySQL이 자체적으로 데이터 절단 에러를 던져 최종 재고·성공 건수 숫자 자체는 우연히
 * 똑같이 나온다 — 즉 이중 안전장치가 있다. 다만 그 경우 앱은 조용한 "재고 0"이 아니라 SQL 예외로
 * 죽으므로(같은 트랜잭션에 있는 결제 완료 처리까지 통째로 롤백될 위험), 아래 테스트는 최종 상태뿐
 * 아니라 "예외 없이 끝났는가"까지 같이 확인해야 가드 유무를 실제로 구분해낸다.
 */
@SpringBootTest
class ProductOptionConcurrencyTest {

    @Autowired
    private ProductOptionRepository productOptionRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final int INITIAL_STOCK = 5;
    private static final int CONCURRENT_REQUESTS = 20; // 재고의 4배 요청을 동시에 보냄

    private Long productId;
    private Long optionId;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("테스트 DB에 카테고리 시드 데이터가 없습니다."));

        Product product = Product.builder()
                .category(category)
                .name("[TEST] 동시성 테스트 상품")
                .brand("test")
                .basePrice(1000)
                .description("동시성 테스트용 임시 상품 — 테스트 종료 시 삭제됨")
                .thumbnailUrl("about:blank")
                .build();
        // DB(Django 마이그레이션 소유 스키마)에 collection이 NOT NULL이라 세팅 — 엔티티에
        // 빌더/세터가 없어(운영 코드는 항상 Django가 채운 값을 읽기만 함) 테스트에서만 리플렉션 사용.
        ReflectionTestUtils.setField(product, "collection", "main");
        product = productRepository.save(product);
        ProductOption option = productOptionRepository.save(ProductOption.builder()
                .product(product)
                .optionName("test")
                .optionValue("test")
                .extraPrice(0)
                .stockCount(INITIAL_STOCK)
                .build());

        productId = product.getId();
        optionId = option.getId();
    }

    @AfterEach
    void tearDown() {
        productOptionRepository.deleteById(optionId);
        productRepository.deleteById(productId);
    }

    @Test
    void 재고보다_많은_동시_요청이_와도_재고는_음수가_되지_않고_딱_재고만큼만_성공한다() throws InterruptedException {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_REQUESTS);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger exceptionCount = new AtomicInteger();

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    try {
                        Integer updated = txTemplate.execute(status ->
                                productOptionRepository.decrementStockIfAvailable(optionId, 1));
                        if (updated != null && updated > 0) {
                            successCount.incrementAndGet();
                        }
                    } catch (RuntimeException e) {
                        // 가드가 살아있으면 절대 여기 안 옴 — 재고 부족은 예외가 아니라 반환값 0으로 표현됨.
                        exceptionCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown(); // 모든 스레드를 한 번에 출발시켜 경쟁 상태를 실제로 유발
        boolean finished = done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).as("모든 스레드가 제시간 안에 끝나야 함").isTrue();
        assertThat(exceptionCount.get())
                .as("재고 부족은 예외가 아니라 반환값 0으로 조용히 처리돼야 함(그래야 결제 완료 트랜잭션이 안 죽음)")
                .isZero();
        assertThat(successCount.get()).as("성공한 차감 건수는 정확히 초기 재고 수와 같아야 함").isEqualTo(INITIAL_STOCK);

        ProductOption after = productOptionRepository.findById(optionId).orElseThrow();
        assertThat(after.getStockCount()).as("재고는 절대 음수가 되면 안 됨").isEqualTo(0);
    }
}
