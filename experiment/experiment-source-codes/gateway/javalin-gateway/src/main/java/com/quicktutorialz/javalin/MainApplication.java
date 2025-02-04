package com.quicktutorialz.javalin;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quicktutorialz.javalin.domain.auth.AuthRequest;
import com.quicktutorialz.javalin.domain.http.HttpMethod;
import com.quicktutorialz.javalin.domain.service.Service;
import com.quicktutorialz.javalin.domain.auth.AuthResponse;
import com.quicktutorialz.javalin.domain.service.ServiceRegistry;
import io.javalin.Context;
import io.javalin.Javalin;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.quicktutorialz.javalin.domain.encrypt.EncryptionUtils.encrypt;
import static com.quicktutorialz.javalin.domain.env.EnvVarRegistry.getEnv;
import static com.quicktutorialz.javalin.domain.http.HttpMethod.*;
import static com.quicktutorialz.javalin.domain.http.HttpUtils.*;
import static com.quicktutorialz.javalin.domain.jwt.JwtUtils.generateJwt;
import static com.quicktutorialz.javalin.domain.jwt.JwtUtils.verifyJwt;

public class MainApplication {

    private static Logger log = LoggerFactory.getLogger(MainApplication.class);

    private static ObjectMapper mapper = new ObjectMapper();

    /*
        REGISTER NEW SERVICE
        curl -X POST 'http://localhost:4000/javalin-api-gateway/services/register' -H 'Content-Type:application/json; charset:utf-8' -d '{"code":"hipster","hostUrl":"http://www.hipster.com"}'

        ORIGINAL SERVICE CALL
        curl -X POST 'http://localhost:7000/javalin-api/you' -H 'Content-Type:application/json; charset:utf-8' -d '{"title":"mr.","name":"alessandro","surname":"argentieri"}'

        API GATEWAY TO REVERSE PROXY TO THE SERVICE
        curl -X POST 'http://localhost:4000/javalin-api-gateway/javalin-api/you' -H 'Content-Type:application/json; charset:utf-8' -d '{"title":"mr.","name":"alessandro","surname":"argentieri"}'
     */

    public static void main(String[] args) {

        /* ~~~~~~~~~~~~~~~~~~~~~~~~~ app declaration ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

        Javalin app = Javalin.create()
                .contextPath("/javalin-api-gateway")
                .enableAutogeneratedEtags()
                .enableCorsForAllOrigins()
                .start(4000);

        /* ~~~~~~~~~~~~~~~~~~~~~~ new service registration ~~~~~~~~~~~~~~~~~~~~~~~~~~*/

        app.post("/services/register", ctx-> {
            if(Objects.equals(ctx.header("Authorization"), getEnv("SERVICE_REGISTRY_SECRET"))) {
                Service service = ctx.validatedBodyAsClass(Service.class).getOrThrow();
                ServiceRegistry.serviceMapping.put(service.getCode(), service.getHostUrl());
                ctx.result("Service ".concat(service.getCode()).concat(" correctly registered. "));
            } else {
                ctx.status(400);
                ctx.result("You have not the authorization to register a new service. ");
            }
        });


        app.get("/envs", ctx-> {
            String response = "Response: "
                    .concat("frontendHost = ").concat( ctx.queryParam("frontendHost") ).concat("; ")
                    .concat("redirectUrl = ").concat( ctx.queryParam("redirectUrl") ).concat("; ")
                    .concat("gatewayHost = ").concat( ctx.queryParam("gatewayHost") ).concat("; ")
                    .concat("gatewayUrl = ").concat( ctx.queryParam("gatewayUrl") ).concat("; ")
                    .concat("gatewayLoginUrl = ").concat( ctx.queryParam("gatewayLoginUrl") ).concat("; ")
                    .concat("temporaryCodeUrl = ").concat( ctx.queryParam("temporaryCodeUrl") ).concat("; ");

            ctx.result(response);
        });

        app.post("/login", ctx-> {
            ctx.result(CompletableFuture.supplyAsync(()->{

                String clientSecret    = getEnv("OAUTH2_CLIENT_SECRET");
                String accessTokenUrl  = getEnv("OAUTH2_ACCESS_TOKEN_URL");
                String apiProviderUrl  = getEnv("OAUTH2_PROVIDER_URL");
                //String clientSecret    = "6dad129bc14cca40ec61949a9ed20a0bbbff3136"; //getEnv("OAUTH2_CLIENT_SECRET");
                //String accessTokenUrl  = "https://github.com/login/oauth/access_token"; //getEnv("OAUTH2_ACCESS_TOKEN_URL");
                //String apiProviderUrl = "https://api.github.com"; // getEnv("OAUTH2_PROVIDER_URL");

                AuthRequest authRequest = ctx.validatedBodyAsClass(AuthRequest.class).getOrThrow();
                String authUrl = getAccessTokenUrl(clientSecret, accessTokenUrl, authRequest);

                try {
                    InputStream stream = post(authUrl, ctx.headerMap(), "application/json", null);
                    String[] resultSet = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n")).split("&");

                    String accessToken = resultSet[0].replace("access_token=","");
                    String scope = resultSet[1].replace("scope=","");
                    String userUrl = apiProviderUrl.concat("/").concat(scope);
                    Map<String, String> userHeaders = new HashMap<>();
                    userHeaders.put("Authorization", "token ".concat(accessToken));

                    Map<String, Object> userMap = mapper.readValue(get(userUrl, userHeaders, "application/json"), Map.class);
                    String id = String.valueOf((Integer) userMap.get("id"));
                    String name = (String) userMap.get("name");
                    String jwt = generateJwt( encrypt(id, name, accessToken) );

                    ctx.header("jwt", jwt);
                    return new ByteArrayInputStream(mapper.writeValueAsString(new AuthResponse(id, name, jwt)).getBytes());
                } catch (IOException e) {
                    return new ByteArrayInputStream(e.getMessage().getBytes());
                }
            }));
        });


        /* ~~~~~~~~~~~~~~~~~~~~~~~~~proxy http methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

        app.get("/*", ctx-> {
            ctx.result(CompletableFuture.supplyAsync(() -> proxyRequest(ctx, GET) ));
        });

        app.post("/*", ctx-> {
            ctx.result(CompletableFuture.supplyAsync(() -> proxyRequest(ctx, POST) ));
        });

        app.put("/*", ctx-> {
            ctx.result(CompletableFuture.supplyAsync(() -> proxyRequest(ctx, PUT) ));
        });

        app.delete("/*", ctx-> {
            ctx.result(CompletableFuture.supplyAsync(() -> proxyRequest(ctx, DELETE) ));
        });

        app.patch("/*", ctx-> {
            ctx.result(CompletableFuture.supplyAsync(() -> proxyRequest(ctx, PATCH)));
        });

        app.head("/*", ctx-> {
            ctx.result(CompletableFuture.supplyAsync(() -> proxyRequest(ctx, HEAD)));
        });

        /* ~~~~~~~~~~~~~~~~~~~~~~~~~ filtering methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/


        //filtering all
        app.before(ctx->{
            log.info(ctx.path());
            log.info(ctx.ip());
        });

        //after Responding to a Path
        app.after(ctx->{
            log.info(ctx.url());
        });

    }


    @NotNull
    private static String getAccessTokenUrl(String clientSecret, String accessTokenUrl, AuthRequest authRequest) {
        return accessTokenUrl
                .concat("?client_id=")
                .concat(authRequest.getClientId())
                .concat("&client_secret=")
                .concat(clientSecret)
                .concat("&code=")
                .concat(authRequest.getCode())
                .concat("&redirect_uri=")
                .concat(authRequest.getRedirectUri())
                .concat("&state=")
                .concat(authRequest.getState());
    }

    /* ~~~~~~~~~~~~~~~~~~~~~~~~~utility methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    private static String getMediaType(Context ctx) {
        return ctx.header("Content-Type");
    }

    private static String getServiceUrl(final Context ctx) {
        String[] splittedUrl = ctx.url().split("/javalin-api-gateway");
        String serviceName = splittedUrl[1].split("/")[1];
        String queryString = ctx.queryString()!=null ? "?".concat(ctx.queryString()) : "";
        return ServiceRegistry.serviceMapping.get(serviceName).concat(splittedUrl[1]).concat(queryString);
    }

    public static InputStream post(String url, Map<String, String> headers, String mediaType, byte[] content) {
        return performRequest(composePostRequest(url, headers, mediaType, content));
    }

    public static InputStream get(String url, Map<String, String> headers, String mediaType) {
        headers.put("Content-Type", mediaType);
        return performRequest(composeGetRequest(url, headers));
    }

    private static InputStream performRequest(Request request) {
        try {
            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();
            return response.body().byteStream();
        } catch (Exception e) {
            return new ByteArrayInputStream(e.getMessage().getBytes());
        }
    }

    private static InputStream proxyRequest(Context ctx, HttpMethod httpMethod) {
        Request request = composeProxyRequest(ctx, httpMethod);
        try {
            checkAuth(ctx);
            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();
            removeToken(ctx);
            return response.body().byteStream();
        } catch (Exception e) {
            return errorReturn(ctx, e);
        }
    }

    private static void checkAuth(Context ctx) {

        String authorization = ctx.header("Authorization");
        if(authorization == null) {
            throw  new JWTVerificationException("Unauthorized");
        }
        String jwt = authorization.replace("Bearer ", "");
        String decoded = verifyJwt(jwt);
        if(decoded == null) {
            throw  new JWTVerificationException("Session expired");
        }
        ctx.header("token", decoded);
    }

    private static InputStream errorReturn(Context ctx, Exception e) {
        if(e instanceof JWTVerificationException) {
            ctx.status(400);
        } else {
            ctx.status(500);
        }
        removeToken(ctx);
        return new ByteArrayInputStream(e.getMessage().getBytes());
    }

    private static void removeToken(Context ctx) {
        ctx.headerMap().remove("token");
    }

    private static Request composeProxyRequest(Context ctx, HttpMethod httpMethod) {
        switch(httpMethod) {
            case GET:
                return composeGetRequest(getServiceUrl(ctx), ctx.headerMap());
            case POST:
                return composePostRequest(getServiceUrl(ctx), ctx.headerMap(), getMediaType(ctx), ctx.bodyAsBytes());
            case PUT:
                return composePutRequest(getServiceUrl(ctx), ctx.headerMap(), getMediaType(ctx), ctx.bodyAsBytes());
            case PATCH:
                return composePatchRequest(getServiceUrl(ctx), ctx.headerMap(), getMediaType(ctx), ctx.bodyAsBytes());
            case DELETE:
                return composeDeleteRequest(getServiceUrl(ctx), ctx.headerMap());
            case HEAD:
                return composeHeadRequest(getServiceUrl(ctx), ctx.headerMap());
            default:
                return composeGetRequest(getServiceUrl(ctx), ctx.headerMap());
        }
    }

}
