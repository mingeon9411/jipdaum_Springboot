package com.jipdaum_spring.service;

import com.jipdaum_spring.dto.auth.IdentityVerificationVerifyRequest;
import com.jipdaum_spring.dto.common.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Service
public class IdentityVerificationService {

    @Value("${portone.api-secret}")
    private String portoneApiSecret;

    public MessageResponse verify(IdentityVerificationVerifyRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + portoneApiSecret);
        URI uri = UriComponentsBuilder
                .fromUriString("https://api.portone.io/identity-verifications/{id}")
                .buildAndExpand(request.identityVerificationId())
                .encode()
                .toUri();

        Map<?, ?> portoneData;
        try {
            ResponseEntity<Map> response = new RestTemplate().exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            portoneData = response.getBody();
        } catch (HttpStatusCodeException e) {
            log.warn("PortOne 본인인증 검증 API 호출 실패 - identityVerificationId={}, status={}",
                    request.identityVerificationId(), e.getStatusCode().value());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "PortOne 본인인증 검증에 실패했습니다.");
        } catch (Exception e) {
            log.warn("PortOne 본인인증 검증 API 호출 실패 - identityVerificationId={}",
                    request.identityVerificationId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "PortOne 본인인증 검증에 실패했습니다.");
        }

        String status = portoneData != null && portoneData.get("status") instanceof String value
                ? value : null;
        if (!"VERIFIED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "본인인증이 완료되지 않았습니다.");
        }

        return new MessageResponse("본인인증이 완료되었습니다.");
    }
}
