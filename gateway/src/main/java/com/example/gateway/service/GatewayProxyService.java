package com.example.gateway.service;

import com.example.gateway.crypto.ChecksumModule;
import com.example.gateway.crypto.CryptoModule;
import com.example.gateway.config.ApiRoute;
import com.example.gateway.util.RouteUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;

@Service
public final class GatewayProxyService {

    private final CryptoModule cryptoModule;
    private final ChecksumModule checksumModule;
    private final RouteResolver routeResolver;
    private final GatewayHeaderFactory gatewayHeaderFactory;
    private final UpstreamClient upstreamClient;
    private final ResponseBodyDecryptor responseBodyDecryptor;

    public GatewayProxyService(CryptoModule cryptoModule,
                               ChecksumModule checksumModule,
                               RouteResolver routeResolver,
                               GatewayHeaderFactory gatewayHeaderFactory,
                               UpstreamClient upstreamClient,
                               ResponseBodyDecryptor responseBodyDecryptor) {
        this.cryptoModule = cryptoModule;
        this.checksumModule = checksumModule;
        this.routeResolver = routeResolver;
        this.gatewayHeaderFactory = gatewayHeaderFactory;
        this.upstreamClient = upstreamClient;
        this.responseBodyDecryptor = responseBodyDecryptor;
    }

    public ResponseEntity<String> proxyPost(String org, String service, String api, String plainBody) {
        ApiRoute route = routeResolver.resolve(org, service, api, "POST");

        String encryptedData = cryptoModule.encrypt(route.key(), plainBody);
        String checksum = checksumModule.checksum(encryptedData);

        URI targetUri = RouteUtils.buildTargetUri(route);
        var headers = gatewayHeaderFactory.build(route, checksum);

        Map<String, Object> requestBody = Map.of("data", encryptedData);

        ResponseEntity<String> upstreamResponse = upstreamClient.post(targetUri, headers, requestBody);
        String decryptedBody = responseBodyDecryptor.decrypt(route, upstreamResponse.getBody());

        return ResponseEntity.status(upstreamResponse.getStatusCode()).body(decryptedBody);
    }
}
