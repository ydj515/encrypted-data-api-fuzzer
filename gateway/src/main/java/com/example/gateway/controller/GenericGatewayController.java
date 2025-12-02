package com.example.gateway.controller;

import com.example.gateway.service.GatewayProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cats")
public class GenericGatewayController {

    private final GatewayProxyService gatewayProxyService;

    public GenericGatewayController(GatewayProxyService gatewayProxyService) {
        this.gatewayProxyService = gatewayProxyService;
    }

    @PostMapping("/{org}/{service}/{api}")
    public ResponseEntity<String> proxyPost(@PathVariable String org,
                                            @PathVariable String service,
                                            @PathVariable String api,
                                            @RequestBody String plainBody) {
        return gatewayProxyService.proxyPost(org, service, api, plainBody);
    }
}
