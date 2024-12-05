package com.mediplanpro.filter;

import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import java.net.URL;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

@WebFilter(urlPatterns = "/*")
public class TokenAuthenticationFilter implements Filter {


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 检查 Authorization 头
        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 提取 token
            String token = authHeader.substring(7);

            // 验证 token
            if (validateToken(token)) {
                // 如果验证通过，继续处理请求
                chain.doFilter(request, response);
                return;
            }
        }

        // 如果没有 token 或 token 无效，返回 401 未授权
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean validateToken(String token) {
        String googleVerifyUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
        try {
            URL url = new URL(googleVerifyUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) { // 如果响应码为 200，表示令牌有效
                // 可以进一步解析响应体中的用户信息
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                conn.disconnect();

                // 在这里解析 JSON 响应，检查特定用户信息或权限
                // 例如解析 content.toString() 中的 'aud', 'exp', 'email' 等字段

                return true; // 令牌验证成功
            } else {
                return false; // 令牌无效
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false; // 令牌无效
        }
    }

}

