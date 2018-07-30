package cn.kkl.mall.cart.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import cn.kkl.mall.pojo.TbUser;
import cn.kkl.mall.sso.service.TokenService;
import cn.kkl.mall.utils.CookieUtils;

/**
 * @author Admin
 * loginInterceptor logic:
 * 1. get token from cookie,
 *    if can not get,we judged as not login in,release directly.
 * 	  if get it,we can invoke sso service,get user information by token.
 * 		if can not get user information,login expired ,release directly
 * 		if get it,user state is login,set user information in request.we judge if contain user information in controller
 * 2.release
 */
public class LoginInterceptor implements HandlerInterceptor {
	
	@Autowired
	private TokenService tokenService;

	/* 
	 * after return modelAndView ,we can deal with exception
	 */
	@Override
	public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
			throws Exception {

	}

	/* after handler execute before return modelAndView 
	 * 
	 */
	@Override
	public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
			throws Exception {

	}

	/* 
	 * execute this method before execute handler
	 * if return true , release, execute handler
	 * if return false, intercept,no longer execute follow-up code
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String token = CookieUtils.getCookieValue(request, "token");
		if (StringUtils.isBlank(token)) {
			return true;
		}
		TbUser tbUser = tokenService.getUserByToken(token);
		if (tbUser==null) {
			return true;
		}
		request.setAttribute("user", tbUser);
		return true;
	}

}
