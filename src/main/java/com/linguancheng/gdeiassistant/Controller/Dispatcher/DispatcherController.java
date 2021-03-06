package com.linguancheng.gdeiassistant.Controller.Dispatcher;

import com.linguancheng.gdeiassistant.Pojo.Entity.User;
import com.linguancheng.gdeiassistant.Pojo.Redirect.RedirectInfo;
import com.linguancheng.gdeiassistant.Pojo.UserLogin.UserCertificate;
import com.linguancheng.gdeiassistant.Service.UserData.UserDataService;
import com.linguancheng.gdeiassistant.Tools.HttpClientUtils;
import com.linguancheng.gdeiassistant.Tools.StringEncryptUtils;
import com.linguancheng.gdeiassistant.Service.UserLogin.AutoLoginService;
import com.linguancheng.gdeiassistant.Service.UserLogin.UserLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by linguancheng on 2017/7/17.
 */

@Controller
public class DispatcherController {

    private final int AUTOLOGIN_NOT = 0;
    private final int AUTOLOGIN_SESSION = 1;
    private final int AUTOLOGIN_COOKIE = 2;

    @Autowired
    private AutoLoginService autoLoginService;

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private UserDataService userDataService;

    @RequestMapping(value = "/")
    public ModelAndView DoDispatch(HttpServletRequest request, HttpServletResponse response
            , RedirectInfo redirectInfo) throws Exception {
        //检查自动登录状态
        ModelAndView modelAndView = new ModelAndView();
        int autoLoginState = autoLoginService.CheckAutoLogin(request);
        if (autoLoginState == AUTOLOGIN_NOT) {
            //不自动登录,返回登录主页
            if (redirectInfo.needToRedirect()) {
                modelAndView.setViewName("redirect:/login?redirect_url=" + redirectInfo.getRedirect_url());
            } else {
                modelAndView.setViewName("redirect:/login");
            }
        } else if (autoLoginState == AUTOLOGIN_COOKIE) {
            //获取Cookie中的用户信息自动登录
            Cookie cookies[] = request.getCookies();
            String cookieUsername = "";
            String cookiePassword = "";
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("username")) {
                    cookieUsername = cookie.getValue();
                }
                if (cookie.getName().equals("password")) {
                    cookiePassword = cookie.getValue();
                }
            }
            //将用户信息进行解密
            cookieUsername = StringEncryptUtils.decryptString(cookieUsername);
            cookiePassword = StringEncryptUtils.decryptString(cookiePassword);
            //清除已登录用户的用户凭证记录
            HttpClientUtils.ClearHttpClientCookieStore(request.getSession().getId());
            //进行用户登录
            UserCertificate userCertificate = userLoginService.UserLogin(request.getSession().getId()
                    , new User(cookieUsername, cookiePassword), true);
            //同步数据库用户数据
            userDataService.SyncUserData(userCertificate.getUser());
            //将用户信息数据写入Session
            request.getSession().setAttribute("username", userCertificate.getUser().getUsername());
            request.getSession().setAttribute("password", userCertificate.getUser().getPassword());
            userLoginService.AsyncUpdateSession(request);
            //将加密的用户信息保存到Cookie中
            String username = StringEncryptUtils.encryptString(userCertificate.getUser().getUsername());
            String password = StringEncryptUtils.encryptString(userCertificate.getUser().getPassword());
            Cookie usernameCookie = new Cookie("username", username);
            Cookie passwordCookie = new Cookie("password", password);
            //设置Cookie最大有效时间为3个月
            usernameCookie.setMaxAge(7776000);
            usernameCookie.setPath("/");
            usernameCookie.setHttpOnly(true);
            passwordCookie.setMaxAge(7776000);
            passwordCookie.setPath("/");
            passwordCookie.setHttpOnly(true);
            response.addCookie(usernameCookie);
            response.addCookie(passwordCookie);
            if (redirectInfo.needToRedirect()) {
                modelAndView.setViewName("redirect:" + redirectInfo.getRedirect_url());
            } else {
                modelAndView.setViewName("redirect:/index");
            }
        } else {
            if (redirectInfo.needToRedirect()) {
                modelAndView.setViewName("redirect:/" + redirectInfo.getRedirect_url());
            } else {
                modelAndView.setViewName("redirect:/index");
            }
        }
        return modelAndView;
    }
}
