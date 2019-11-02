package com.infinitycare.health.login.model;

import com.infinitycare.health.security.TextSecurer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceUtility {

    public static final String EMAIL_ID = "emailID";
    public static final String OTP = "otp";
    public static final String SESSIONID = "sessionid";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String PATIENT = "patient";
    public static final String DOCTOR = "doctor";
    public static final String INSURANCE_PROVIDER = "insurance";
    public static final String IS_CREDENTIALS_ACCURATE = "isCredentialsAccurate";
    public static final String IS_OTP_SENT = "isOtpSent";
    public static final String IS_OTP_ACCURATE = "isOtpAccurate";
    public static final String IS_COOKIE_TAMPERED = "isCookieTampered";
    public static final String IS_NEW_USER = "isNewUser";
    public static final String IS_USER_EXISTS = "isUserExists";
    public static final String IS_PASSWORD_CHANGED = "isPasswordChanged";

    public Map<String, String> getPostBodyInAMap(HttpServletRequest request) {
        Map<String, String> postBody = new HashMap<>();
        try {
            populatePostBody(postBody, request.getReader().lines().collect(Collectors.joining()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return postBody;
    }

    private void populatePostBody(Map<String, String> parameterMap, String body) {
        try {
            JSONObject userDetails = (JSONObject) new JSONParser().parse(body);
            for(Object key: userDetails.keySet()) {
                parameterMap.put((String)key, (String)userDetails.get(key));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void setEncryptedSessionId(HttpServletRequest request, HttpServletResponse response, String username, String userType) {
        String encryptedSessionId = TextSecurer.encrypt(username);
        String servletPath = request.getServletPath();
        String cookiePath = servletPath.substring(0, servletPath.indexOf(userType) + userType.length());

        Cookie cookie = new Cookie(SESSIONID, encryptedSessionId);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(-1);
        cookie.setPath(cookiePath);

        //response.setHeader("Set-Cookie", SESSIONID + "=" + encryptedSessionId + "; Path=" + cookiePath + "; HttpOnly; SameSite=Lax");

        response.addCookie(cookie);
    }

}
