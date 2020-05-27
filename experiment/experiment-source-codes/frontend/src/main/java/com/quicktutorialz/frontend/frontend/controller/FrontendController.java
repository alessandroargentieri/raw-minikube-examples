package com.quicktutorialz.frontend.frontend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
public class FrontendController {

    @Value("${clientId}")
    private String clientId;

    @Value("${redirectUrl}")
    private String redirectUrl;

    @Value("${state}")
    private String state;

    @Value("${gatewayLoginUrl}")
    private String gatewayLoginUrl;

    @Value("${securedCallUrl}")
    private String securedCallUrl;

    @Value("${temporaryCodeUrl}")
    private String temporaryCodeUrl;

    @Value("${frontendHost}")
    private String frontendHost;

    @Value("${gatewayHost}")
    private String gatewayHost;

    @Value("${gatewayUrl}")
    private String gatewayUrl;

    @CrossOrigin
    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("temporaryCodeUrl", temporaryCodeUrl);
        return "home";
    }

    @CrossOrigin
    @GetMapping("/login/oauth2/code/github")
    public String redirectAuth(String code, Model model) {

        model.addAttribute("code", code);
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUrl", redirectUrl);
        model.addAttribute("state", state);
        model.addAttribute("gatewayLoginUrl", gatewayLoginUrl);
        model.addAttribute("securedCallUrl", securedCallUrl);

        return "login";
    }

    @CrossOrigin
    @GetMapping("env/{name}")
    @ResponseBody
    public String getEnvValue(@PathVariable(name = "name") String name) {
        if (name.equals("frontendHost")) {
            return frontendHost;
        } else if(name.equals("redirectUrl")) {
            return redirectUrl;
        } else if(name.equals("gatewayHost")) {
            return gatewayHost;
        } else if(name.equals("gatewayUrl")) {
            return gatewayUrl;
        } else if(name.equals("gatewayLoginUrl")) {
            return gatewayLoginUrl;
        } else if(name.equals("temporaryCodeUrl")) {
            return temporaryCodeUrl;
        } else {
            return "frontendHost|redirectUrl|gatewayHost|gatewayUrl|gatewayLoginUrl|temporaryCodeUrl";
        }
    }

    @CrossOrigin
    @GetMapping("gateway/envs")
    @ResponseBody
    public String getGatewaysEnvValues() {

        RestTemplate restTemplate = new RestTemplate();
        String url = gatewayUrl.concat("/envs")
                               .concat("?frontendHost=").concat(frontendHost)
                               .concat("&redirectUrl=").concat(redirectUrl)
                               .concat("&gatewayHost=").concat(gatewayHost)
                               .concat("&gatewayUrl=").concat(gatewayUrl)
                               .concat("&gatewayLoginUrl=").concat(gatewayLoginUrl)
                               .concat("&temporaryCodeUrl=").concat(temporaryCodeUrl);

        return restTemplate.getForObject(url, String.class);
    }


}
