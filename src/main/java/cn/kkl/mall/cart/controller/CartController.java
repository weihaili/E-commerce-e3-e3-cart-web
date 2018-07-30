package cn.kkl.mall.cart.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.kkl.mall.cart.service.CartService;
import cn.kkl.mall.pojo.E3Result;
import cn.kkl.mall.pojo.TbItem;
import cn.kkl.mall.pojo.TbUser;
import cn.kkl.mall.service.ItemService;
import cn.kkl.mall.utils.CookieUtils;
import cn.kkl.mall.utils.JsonUtils;



@Controller
public class CartController {
	
	@Value("${CART_IN_COOKIE_KEY}")
	private String cartListInCookieKey;
	
	@Value("${CART_IN_COOKIE_EXPIRED_TIEM}")
	private Integer cartListSaveInCookieLimit;
	
	@Autowired
	private ItemService itemService;
	
	@Autowired
	private CartService cartService;

	
	/**
	 * cart implementation logic:
	 * 1. get cart list from cookie
	 * 2. determine if the item exists in the cart list
	 * 3. if exists then add up the quantity
	 * 4. if does not exists ,query item information dependents on itemId from database then get tbItem instance
	 * 5. add this item information to cart list
	 * 6. judge user login status,if login write cart list to redis 
	 * 7. if not , write the cart list to cookie again
	 * 8. return add success page 
	 * @param itemId
	 * @param num
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/cart/add/{itemId}")
	public String addItem(@PathVariable Long itemId,@RequestParam(defaultValue="1")Integer num,
			HttpServletRequest request,HttpServletResponse response) {
		TbUser user=(TbUser) request.getAttribute("user");
		if (user!=null) {
			E3Result result = cartService.addCart(user.getId(), itemId, num);
			if (result.getStatus()==200) {
				return "cartSuccess";
			}
		}
		List<TbItem> cartList = getCartListFromCoookie(request);
		boolean flag=false;
		for (TbItem tbItem : cartList) {
			if (tbItem.getId()==itemId.longValue()) {
				flag=true;
				tbItem.setNum(tbItem.getNum()+num);
				break;
			}
		}
		if (!flag) {
			TbItem item = itemService.getItemById(itemId);
			item.setNum(num);
			item.setImage(StringUtils.isBlank(item.getImage().split(",")[0])?"":item.getImage().split(",")[0]);
			cartList.add(item);
		}
		CookieUtils.setCookie(request, response, cartListInCookieKey, JsonUtils.objectToJson(cartList), cartListSaveInCookieLimit ,true);
		return "cartSuccess";
	}
	
	/**
	 * display cart list logic:
	 * a. judge user login status:
	 *    if login: 
	 *    	1. get cart list from cookie,judge the cart list is it empty:
	 *    		if it is not empty combine cookie cart list with reids cart list
	 *    		the same item add up the quantity
	 *    		different item add item
	 *    	2. delete cart list in cookie 
	 * b. get redis cart list return display page   
	 *    if does not login
	 * 1. get cart list from cookie
	 * 2. transfer this cart list to display page
	 * 3. return logic view
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping("/cart/cart")
	public String showCartList(HttpServletRequest request,HttpServletResponse response) {
		List<TbItem> list = getCartListFromCoookie(request);
		TbUser user=(TbUser) request.getAttribute("user");
		if (user!=null) {
			cartService.mergeCart(user.getId(), list);
			CookieUtils.deleteCookie(request, response, cartListInCookieKey);
			list = cartService.getCartList(user.getId());
		}
		request.setAttribute("cartList", list);
		return "cart";
	}
	
	/**
	 * update cart item count logic:
	 * 1. get cart list from cookie
	 * 2. polling cart list find the item which itemId equals to parameter itemId
	 * 3. update this item number
	 * 4. write this cart list to cookie
	 * 5. return success
	 * @return
	 */
	@RequestMapping(value="/cart/update/num/{itemId}/{num}",method=RequestMethod.POST)
	@ResponseBody
	public E3Result updateCartItemNumber(@PathVariable Long itemId,@PathVariable Integer num,
			HttpServletRequest request,HttpServletResponse response) {
		TbUser user=(TbUser) request.getAttribute("user");
		if (user!=null) {
			E3Result e3Result = cartService.updateCartNum(user.getId(), itemId, num);
			return e3Result;
		}
		List<TbItem> list = getCartListFromCoookie(request);
		for (TbItem tbItem : list) {
			if (tbItem.getId()==itemId.longValue()) {
				tbItem.setNum(num);
				break;
			}
		}
		CookieUtils.setCookie(request, response, cartListInCookieKey, JsonUtils.objectToJson(list), cartListSaveInCookieLimit, true);
		return E3Result.ok();
	}
	
	/**
	 * delete item in cart List logic:
	 * 1. get cart list from cookie
	 * 2. polling the cart list,get the item which itemId is parameter itemId, remove this item
	 * 3. write the item cart list to cookie
	 * 4. redirect url "/cart/cart" display cart list
	 * @param itemId
	 * @return
	 */
	@RequestMapping("/cart/delete/{itemId}")
	public String deleteItemInCartList(HttpServletRequest request,@PathVariable Long itemId,HttpServletResponse response) {
		TbUser user=(TbUser) request.getAttribute("user");
		if (user!=null) {
			E3Result e3Result = cartService.deleteCartItem(user.getId(), itemId);
			if (e3Result.getStatus()==200) {
				return "redirect:/car/cart.html";
			}else {
				request.setAttribute("message", e3Result.getData());
				return "error";
			}
		}
		List<TbItem> list = getCartListFromCoookie(request);
		for (TbItem tbItem : list) {
			if (tbItem.getId()==itemId.longValue()) {
				list.remove(tbItem);
				break;
			}
		}
		CookieUtils.setCookie(request, response, cartListInCookieKey, JsonUtils.objectToJson(list), cartListSaveInCookieLimit, true);
		return "redirect:/car/cart.html";
	}
	
	/**
	 * get cart list from cookie
	 * @param request
	 * @return
	 */
	private List<TbItem> getCartListFromCoookie(HttpServletRequest request){
		String cookieValue = CookieUtils.getCookieValue(request, cartListInCookieKey, true);
		if (StringUtils.isBlank(cookieValue)) {
			return new ArrayList<TbItem>();
		}
		List<TbItem> list = JsonUtils.jsonToList(cookieValue, TbItem.class);
		return list;
	}
	
	
}
