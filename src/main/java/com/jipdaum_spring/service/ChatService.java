package com.jipdaum_spring.service;

import com.jipdaum_spring.dto.chat.ChatMessage;
import com.jipdaum_spring.service.chat.ChatTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // 프론트가 매 요청마다 통째로 보내는 history를 이만큼만 사용한다 (payload/비용 방어).
    private static final int MAX_HISTORY_TURNS = 20;

    // 톤/정확성/이관 기준은 기획팀 프롬프트 스펙을 반영한 것. 단, 아래 두 항목은 지금 시스템이
    // 실제로 뒷받침하지 못해서 스펙 그대로 넣지 않고 조정했다(둘 다 그대로 넣으면 오히려 AI가
    // 근거 없는 정보를 지어내게 되어 "할루시네이션 금지" 원칙과 충돌한다):
    //  - 치수(가로/세로/높이)·소재: Product 테이블에 해당 컬럼 자체가 없다. 컬럼/도구 추가 전까지는
    //    "정확한 사이즈는 모른다"고 정직하게 답하도록 하고, 여유공간 권장은 일반 가이드로만 남겼다.
    //  - "OpenAI 데이터 미활용" 문구: 실제 LLM은 OpenAI가 아니라 Gemini(GeminiClient)라 그대로 쓰면
    //    거짓 안내가 된다. Gemini 쪽 데이터 처리 정책이 확정되면 그때 맞는 문구로 채워 넣을 것.
    private static final String SYSTEM_PROMPT = """
            당신은 D2C 가구 브랜드 '집다움'의 전문 인테리어 컨설턴트 AI 챗봇입니다.
            디자이너와 1:1 상담하는 듯한 친절하고 다정한 존댓말(~해요, ~해드릴게요)을 쓰고,
            신뢰감 있게 답변하세요. 딱딱한 AI 말투, 비속어, 근거 없는 답변은 금지입니다.

            [정확성 원칙 — 가장 중요합니다]
            - 상품의 가격/브랜드/설명/카테고리 등 실제 데이터가 필요한 질문에는 반드시 제공된 도구
              (search_products, get_product_detail)로 조회해서 실제 DB 값으로만 답변하세요. 추측 금지.
            - 상품의 정확한 치수(가로/세로/높이)와 소재 정보는 현재 도구로 조회할 수 없습니다.
              지어내지 말고 "정확한 치수·소재는 상품 상세 페이지에서 확인하시거나, 담당자 확인 후
              안내해 드릴게요"라고 답하세요.
            - 그 외에도 확실하지 않은 정보는 절대 지어내지 말고 "해당 문의하신 내용은 담당자 확인 후
              안내해 드리겠습니다."라고 답하세요.

            [공간 중심 추천]
            - 단순 상품 나열이 아니라 "거실 동선을 막지 않으려면 이렇게 배치하면 좋아요" 같은 공간
              활용 팁을 함께 제공하세요.
            - 고객이 배치할 공간의 실측 치수를 알려주면, 문 여닫힘과 통행 동선을 위해 실측 치수보다
              여유 있는 사이즈를 고르시라고 권해주세요. 다만 상품별 정확한 사이즈는 조회할 수 없으니
              이 여유분 권장은 일반적인 가이드로만 안내하고, 특정 상품이 그 공간에 맞는지 단정하지 마세요.

            [정책 준수 — 아래 범위 내에서만 안내]
            - 배송: 평균 2~3 영업일 소요, 전 상품 무료배송
            - 반품/교환: 상품 수령 후 7일 이내, 마이페이지 > 주문내역에서 신청
            - 결제수단: 카카오페이, 네이버페이
            - 쿠폰: 마이페이지 > 쿠폰에서 확인 및 결제 시 적용
            - 회원 등급: MARU → DAUM → JIPUM 순, 구매 금액에 따라 상승
            - 적립금: 구매 금액의 1% 적립, 다음 구매 시 사용 가능
            - 고객센터: 평일 10:00~17:00 운영, 마이페이지 > 1:1 문의로 접수
            이 범위를 벗어나는 배송/반품/AS 질문은 지어내지 말고 상담원 연결로 안내하세요.

            [한계 인식과 상담원 이관]
            - 같은 질문을 2번 이상 이해하지 못했거나, 환불/파손 등 민감한 CS가 접수되면
              "마이페이지 > 1:1 문의"로 상담원 연결을 안내하세요.
            - search_products/get_product_detail로 조건에 맞는 상품을 찾지 못하면, 단순히 없다고
              끝내지 말고 유사한 스타일이나 카테고리로 다시 검색해서 대안 상품을 제시하세요.

            [개인정보 최소 수집]
            - 주문/배송 관련 문의에는 이름이나 연락처 전체를 요청하지 말고 주문번호만 안내받으세요.
              단, 아직 주문 조회나 로그인 연동 기능 자체가 없으니 실시간으로 조회해줄 수 있다고
              말하지 말고, 마이페이지 > 주문내역 이용을 안내하세요.

            집다움과 무관한 질문에는 정중히 답변할 수 없다고 안내하세요.
            """;

    private final GeminiClient geminiClient;
    private final List<ChatTool> chatTools;

    private record Rule(List<String> keywords, List<String> replies) {
    }

    // Django config/chat_view.py의 RULES를 그대로 이식
    private static final List<Rule> RULES = List.of(
            new Rule(List.of("안녕", "hi", "hello", "ㅎㅇ", "반가", "처음", "시작"), List.of(
                    "안녕하세요! 집다움 고객센터입니다 😊 배송, 반품, 쿠폰 등 궁금한 점을 편하게 물어보세요.",
                    "반갑습니다! 집다움입니다 🏠 무엇이든 도와드릴게요.",
                    "안녕하세요~ 집다움 AI 어시스턴트입니다! 궁금한 점이 있으시면 알려주세요 😊"
            )),
            new Rule(List.of("배송", "배달", "언제 와", "얼마나 걸", "도착"), List.of(
                    "주문 완료 후 평균 2~3 영업일 이내 배송됩니다. 도서산간 지역은 1~2일 추가될 수 있어요.",
                    "일반 택배 기준 2~3 영업일 소요됩니다. 주문 후 발송 완료 시 문자로 운송장 번호가 안내됩니다 📦",
                    "배송은 주문 확인 후 1 영업일 내 출고되며, 이후 2~3일 내 수령 가능합니다. 주말·공휴일은 영업일에서 제외됩니다."
            )),
            new Rule(List.of("배송비", "무료배송", "배송 요금"), List.of(
                    "집다움은 전 상품 무료배송을 제공합니다! 🎉",
                    "모든 상품 무료배송입니다. 추가 배송비 없이 편하게 주문하세요 😊"
            )),
            new Rule(List.of("반품", "환불", "교환", "취소", "돌려"), List.of(
                    "상품 수령 후 7일 이내 반품/교환이 가능합니다. 마이페이지 > 주문내역에서 신청해 주세요.",
                    "단순 변심의 경우 7일 이내 반품 가능하며, 왕복 배송비는 고객 부담입니다. 상품 하자의 경우 무료 교환됩니다.",
                    "반품/교환은 상품 수령일 기준 7일 이내 가능합니다. 사용하거나 훼손된 상품은 반품이 어려울 수 있어요."
            )),
            new Rule(List.of("주문", "주문 내역", "결제 내역", "구매 내역", "주문번호"), List.of(
                    "주문 내역은 마이페이지 > 주문내역 조회에서 확인하실 수 있습니다.",
                    "마이페이지에서 주문 현황, 배송 상태, 결제 내역을 모두 확인하실 수 있어요 📋"
            )),
            new Rule(List.of("결제", "카카오페이", "네이버페이", "페이", "신용카드", "계좌"), List.of(
                    "현재 카카오페이, 네이버페이로 결제하실 수 있습니다.",
                    "카카오페이와 네이버페이를 지원합니다. 간편하고 빠르게 결제해 보세요 💳"
            )),
            new Rule(List.of("쿠폰", "할인코드", "프로모션 코드", "할인"), List.of(
                    "보유 쿠폰은 마이페이지 > 쿠폰에서 확인하실 수 있으며, 결제 페이지에서 적용 가능합니다.",
                    "쿠폰 코드는 결제 화면에서 직접 입력하거나, 보유 쿠폰 목록에서 클릭으로 적용할 수 있어요 🎫",
                    "이벤트 쿠폰은 프로모션 페이지에서 받으실 수 있고, 관리자 발급 쿠폰은 마이페이지에서 확인해 주세요."
            )),
            new Rule(List.of("회원", "등급", "maru", "daum", "jipum", "혜택"), List.of(
                    "집다움 회원 등급은 MARU → DAUM → JIPUM 순으로, 구매 금액이 높아질수록 등급이 올라갑니다.",
                    "회원 등급에 따라 쿠폰 및 적립금 혜택이 달라집니다. 더 많이 구매할수록 더 큰 혜택을 누리세요 🌟"
            )),
            new Rule(List.of("적립금", "포인트", "마일리지"), List.of(
                    "구매 금액의 1%가 적립금으로 적립되며, 다음 구매 시 사용 가능합니다.",
                    "적립금은 마이페이지에서 확인하실 수 있고, 결제 시 현금처럼 사용하실 수 있어요 💰"
            )),
            new Rule(List.of("소재", "재질", "크기", "사이즈", "무게", "규격"), List.of(
                    "상품 상세 페이지에서 소재, 크기, 무게 등 상세 정보를 확인하실 수 있습니다.",
                    "각 상품마다 상세 스펙이 등록되어 있어요. 상품 페이지 하단에서 확인해 주세요 📐"
            )),
            new Rule(List.of("재고", "품절", "없어요", "매진", "입고"), List.of(
                    "현재 품절된 상품은 상품 페이지에서 재입고 알림 신청이 가능합니다. 입고 시 문자로 안내해 드려요.",
                    "인기 상품은 빠르게 품절될 수 있습니다. 재입고 알림을 신청해 두시면 놓치지 않을 수 있어요 🔔"
            )),
            new Rule(List.of("문의", "고객센터", "연락", "전화", "상담"), List.of(
                    "고객센터는 평일 10:00~17:00 운영됩니다. 마이페이지 > 1:1 문의를 이용해 주세요.",
                    "1:1 문의는 마이페이지에서 24시간 접수 가능하며, 영업일 기준 1일 내 답변 드립니다 📩"
            )),
            new Rule(List.of("추천", "어떤 상품", "뭐가 좋", "인기", "베스트"), List.of(
                    "집다움의 인기 상품은 상단 메뉴 > 상품에서 확인하실 수 있습니다. 무드 탭에서 공간별 추천도 받아보세요 🏡",
                    "현재 가장 인기 있는 상품들은 홈 화면에서 확인하실 수 있어요. 취향에 맞는 스타일을 찾아보세요 ✨"
            )),
            new Rule(List.of("가격", "얼마", "비용", "금액"), List.of(
                    "상품 가격은 각 상품 상세 페이지에서 확인하실 수 있습니다. 쿠폰 적용 시 추가 할인도 가능해요.",
                    "가격은 상품마다 다릅니다. 상품 목록 또는 상세 페이지에서 확인해 주세요 💡"
            )),
            new Rule(List.of("회원가입", "가입", "로그인", "계정", "비밀번호"), List.of(
                    "회원가입은 우측 상단 아이콘을 통해 하실 수 있습니다. 카카오, 네이버, 구글 소셜 로그인도 지원해요.",
                    "로그인 문제가 있으시면 비밀번호 재설정을 시도해 보세요. 그래도 어려우시면 1:1 문의를 남겨주세요 🔑"
            )),
            new Rule(List.of("감사", "고마", "수고", "좋아", "도움"), List.of(
                    "천만에요! 집다움을 이용해 주셔서 감사합니다 😊 또 궁금한 점이 있으시면 언제든지 물어보세요.",
                    "도움이 되었다니 다행이에요! 집다움에서 좋은 쇼핑 되세요 🏠✨",
                    "감사합니다! 항상 최선을 다하겠습니다. 좋은 하루 되세요 😊"
            ))
    );

    private static final List<String> FALLBACK_REPLIES = List.of(
            "죄송합니다, 정확한 답변을 드리기 어렵습니다. 마이페이지 > 1:1 문의를 이용해 주시면 자세히 안내해 드릴게요 😊",
            "해당 내용은 고객센터(평일 10:00~17:00)에 문의해 주시면 정확한 안내를 받으실 수 있습니다.",
            "죄송해요, 제가 아직 그 부분은 잘 모르겠어요 😅 1:1 문의로 남겨주시면 담당자가 빠르게 답변해 드립니다."
    );

    private static final String CHANNEL_KOREAN_HALL = "korean-hall";

    /**
     * history는 프론트가 들고 있는 이전 대화 턴 (서버 DB에 저장하지 않는 stateless 멀티턴).
     * Gemini 호출이 실패하거나 api-key가 설정되지 않은 환경에서는 기존 규칙 기반 답변으로 폴백한다.
     */
    public String reply(String message, List<ChatMessage> history) {
        return reply(message, history, null);
    }

    /**
     * channel은 프론트 페이지 문맥이다("korean-hall"이면 한국관 페이지, 그 외/null이면 기본 쇼핑몰).
     * search_products가 그 문맥에 맞는 상품 진열(collection)만 검색하도록 고정 인자로 주입한다 —
     * 모델이 스스로 진열을 고르게 두지 않는다(카테고리명이 두 진열에서 겹쳐서 혼동 소지가 있다).
     */
    public String reply(String message, List<ChatMessage> history, String channel) {
        List<ChatMessage> trimmedHistory = history == null
                ? List.of()
                : history.stream()
                        .skip(Math.max(0, history.size() - MAX_HISTORY_TURNS))
                        .toList();

        String collection = CHANNEL_KOREAN_HALL.equals(channel) ? "korean_hall" : "main";
        Map<String, Object> toolContext = Map.of("collection", collection);

        Optional<String> llmReply = geminiClient.generate(SYSTEM_PROMPT, trimmedHistory, message, chatTools, toolContext);
        if (llmReply.isPresent()) {
            return llmReply.get();
        }

        log.info("Gemini 응답을 받지 못해 규칙 기반 답변으로 폴백합니다.");
        return ruleBasedReply(message);
    }

    private String ruleBasedReply(String message) {
        String lower = message.toLowerCase();
        for (Rule rule : RULES) {
            boolean matched = rule.keywords().stream().anyMatch(lower::contains);
            if (matched) {
                return pickRandom(rule.replies());
            }
        }
        return pickRandom(FALLBACK_REPLIES);
    }

    private String pickRandom(List<String> options) {
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
