package pl.stalkon.ad.rest.controller;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pl.stalkon.ad.core.model.User;
import pl.stalkon.ad.core.model.dto.ChangePasswordDto;
import pl.stalkon.ad.core.model.dto.UserProfileDto;
import pl.stalkon.ad.core.model.dto.UserRegForm;
import pl.stalkon.ad.core.model.service.MailService;
import pl.stalkon.ad.core.model.service.UserInfoService;
import pl.stalkon.ad.core.model.service.UserService;
import pl.styall.library.core.rest.ext.MultipleObjectResponse;
import pl.styall.library.core.rest.ext.SingleObjectResponse;

@Controller
public class UserController {
	private static final Logger logger = Logger.getLogger(UserController.class);

	@Autowired
	private UserService userService;

	@Autowired
	private MailService mailService;

	@Autowired
	private UserInfoService userInfoService;
	
//    @RequestMapping(method = RequestMethod.OPTIONS)
//    public void commonOptions(HttpServletResponse theHttpServletResponse) throws IOException {
//        theHttpServletResponse.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, x-requested-with, authtoken");
//        theHttpServletResponse.addHeader("Access-Control-Max-Age", "60"); // seconds to cache preflight request --> less OPTIONS traffic
//        theHttpServletResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
//    }
//	
	@RequestMapping(value = "/user", method = RequestMethod.POST)
	@ResponseBody
	public Long processRegister(@Valid @RequestBody UserRegForm userRegForm) {
		User user = userService.register(userRegForm);
		mailService.sendUserVerificationEmail(user);
		return user.getId();
	}

	@RequestMapping(value = "/user/{userId}", method = RequestMethod.POST)
	@PreAuthorize("principal.id.equals(#userId)")
	@ResponseBody
	public void updateProfile(@PathVariable("userId") Long userId,
			@Valid @RequestBody UserProfileDto userProfileDto) {
		userService.updateProfile(userProfileDto, userId);

	}

	@RequestMapping(value = "/user/{userId}/profile", method = RequestMethod.GET)
	@PreAuthorize("principal.id.equals(#userId)")
	@ResponseBody
	public MultipleObjectResponse getProfilePage(@PathVariable("userId") Long userId) {
		User user = userService.getInitialized(userId);
		MultipleObjectResponse response = new MultipleObjectResponse(2);
		response.put("name",user.getUserData()
				.getName());
		response.put("surname",user.getUserData().getSurname());
		response.put("email",user.getCredentials().getMail());
		return response;
	}

	@RequestMapping(value = "/user/{userId}/password", method = RequestMethod.POST)
	@PreAuthorize("principal.id.equals(#userId)")
	@ResponseBody
	public SingleObjectResponse updatePassword(@PathVariable("userId") Long userId,
			@Valid @RequestBody ChangePasswordDto changePasswordDto) {
		boolean changed = userService.changePassword(userId,
				changePasswordDto.getOldPassword(),
				changePasswordDto.getPassword());
		return new SingleObjectResponse(changed);
	}

	@RequestMapping(value = "/user/email",produces="application/json", method = RequestMethod.GET)
	@ResponseBody
	public SingleObjectResponse checkMailExists(@RequestParam("email") String mail) {
		boolean exists = userService.chechMailExists(mail);
		return new SingleObjectResponse(exists);
	}

	@RequestMapping(value = "/user/username",produces="application/json", method = RequestMethod.GET)
	@ResponseBody
	public SingleObjectResponse checkUsernameExists(@RequestParam("username") String username) {
		boolean exists = userService.chechUsernameExists(username);
		return new SingleObjectResponse(exists);
	}

	@RequestMapping(value = "/user/activate/{token}", method = RequestMethod.GET)
	@ResponseBody
	public SingleObjectResponse activate(@PathVariable("token") String token,
			RedirectAttributes redirectAttributes) {
		boolean activated = userService.activate(token);
		return new SingleObjectResponse(activated);
	}

//	@RequestMapping(value = "/user/{userId}/message", method = RequestMethod.GET)
//	@PreAuthorize("principal.id.equals(#userId)")
//	public List<UserInfo> getMessages(@PathVariable("userId") Long userId) {
//		List<UserInfo> userInfos = userInfoService.getMessages(userId, null);
//		return userInfos;
//	}

//	@RequestMapping(value = "/user/{userId}/address/", method = RequestMethod.POST)
//	@PreAuthorize("principal.id.equals(#userId)")
//	@ResponseBody
//	public UserAddressDto getMessages(@PathVariable("userId") Long userId,
//			@Valid @RequestBody UserAddressDto userAddressDto) {
//		userService.updateUserAddress(userAddressDto, userId);
//		return userAddressDto;
//	}

	@RequestMapping(value = "/user/password/recover", method = RequestMethod.POST)
	@ResponseBody
	public SingleObjectResponse recallPassword(@NotNull @RequestParam("mail") String mail) {
		String newPassword = userService.generateAndSetNewPassword(mail);
		if (newPassword != null) {
			mailService.sendNewPassword(mail, newPassword);
			return new SingleObjectResponse(true);
		}
		return new SingleObjectResponse(false);
	}

}