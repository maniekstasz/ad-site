package pl.stalkon.ad.core.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.jws.HandlerChain;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pl.stalkon.ad.core.model.Ad;
import pl.stalkon.ad.core.model.Ad.Place;
import pl.stalkon.ad.core.model.AdComment;
import pl.stalkon.ad.core.model.Brand;
import pl.stalkon.ad.core.model.Company;
import pl.stalkon.ad.core.model.Contest;
import pl.stalkon.ad.core.model.Contest.State;
import pl.stalkon.ad.core.model.DailymotionData;
import pl.stalkon.ad.core.model.Tag;
import pl.stalkon.ad.core.model.UserRoleDef;
import pl.stalkon.ad.core.model.dao.AdDao;
import pl.stalkon.ad.core.model.dto.AdBrowserWrapper;
import pl.stalkon.ad.core.model.dto.AdPostDto;
import pl.stalkon.ad.core.model.dto.AdSearchDto;
import pl.stalkon.ad.core.model.dto.AutocompleteDto;
import pl.stalkon.ad.core.model.dto.BrandSearchDto;
import pl.stalkon.ad.core.model.dto.CheckboxListWrapper;
import pl.stalkon.ad.core.model.service.AdService;
import pl.stalkon.ad.core.model.service.BrandService;
import pl.stalkon.ad.core.model.service.CompanyService;
import pl.stalkon.ad.core.model.service.ContestService;
import pl.stalkon.ad.core.security.SocialLoggedUser;
import pl.stalkon.ad.core.security.SocialLoggedUser.LoggedType;
import pl.stalkon.ad.extensions.AjaxNotLoggedInException;
import pl.stalkon.ad.social.facebook.SiteFacebookIntegrator;
import pl.stalkon.dailymotion.api.module.service.DailymotionException;
import pl.stalkon.dailymotion.api.module.service.DailymotionService;
import pl.stalkon.video.api.service.VideoApiException;
import pl.stalkon.video.api.service.VideoApiService;
import pl.stalkon.video.api.service.impl.VideoApiServiceImpl;
import pl.stalkon.video.api.service.impl.WistiaApiService;

@Controller
public class AdController {

	@Autowired
	private AdService adService;

	@Autowired
	private BrandService brandService;

	@Autowired
	private VideoApiService videoApiService;

	@Autowired
	private SiteFacebookIntegrator siteFacebookIntegrator;

	@Autowired
	private CompanyService companyService;

	@Autowired
	private ContestService contestService;

	@Autowired
	private Environment env;

	@Autowired
	private ControllerHelperBean controllerHelperBean;

	@RequestMapping(value = "ad/register")
	public String getAddPage(Model model) {
		AdPostDto adDto = new AdPostDto();
		model.addAttribute("adPostDto", adDto);
		model.addAttribute("path", "ad/register");
		return "ad/register";
	}

	@RequestMapping(value = "brand/{brandId}/ad/register")
	public String getAddByBrandPage(Model model, HttpServletRequest request,
			Principal principal, @PathVariable("brandId") Long brandId) {
		SocialLoggedUser socialLoggedUser = (SocialLoggedUser) ((Authentication) principal)
				.getPrincipal();
		Brand brand = brandService.get(brandId);
		Company company = companyService
				.getCompanyWithBrandsByUser(socialLoggedUser.getId());
		if (brand == null || company.getBrands() == null
				|| !company.getBrands().contains(brand)) {
			throw new AccessDeniedException(
					"Nie masz wystarczających uprawnień aby dodać reklamę do tej marki");
		}
		AdPostDto adDto = new AdPostDto();
		model.addAttribute("adPostDto", adDto);
		model.addAttribute("path", "ad/register");
		model.addAttribute("postPath", "/ad");
		model.addAttribute("brand", brand);
		adDto.setBrandId(brandId);
		return "ad/company-register";
	}

	@RequestMapping(value = "contest/{contestId}/ad/register")
	public String getAddAdContestPage(Model model, HttpServletRequest request,
			Principal principal, @PathVariable("contestId") Long contestId) {
		Contest contest = contestService.getWithBrand(contestId);
		AdPostDto adDto = new AdPostDto();
		adDto.setBrandId(contest.getBrand().getId());
		model.addAttribute("adPostDto", adDto);
		model.addAttribute("path", "ad/register");
		model.addAttribute("postPath", "/contest/" + contestId + "/ad");
		model.addAttribute("brand", contest.getBrand());
		return "ad/company-register";
	}

	@RequestMapping(value = "contest/{contestId}/ad", method = RequestMethod.POST)
	public String add(@PathVariable("contestId") Long contestId,
			@Valid @ModelAttribute("adPostDto") AdPostDto adPostDto,
			BindingResult result, Principal principal,
			RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			redirectAttributes
					.addFlashAttribute("info",
							"Źle wprowadzone dane. Proszę postępować zgodnie z zaleceniami");
			return "redirect:/info-page";
		}
		Contest contest = contestService.get(contestId);
		if (!contest.getBrand().getId().equals(adPostDto.getBrandId())
				|| contest.getState() == State.FINISHED) {
			redirectAttributes
					.addFlashAttribute("info",
							"Źle wprowadzone dane. Proszę postępować zgodnie z zaleceniami");
			return "redirect:/info-page";
		}
		SocialLoggedUser socialLoggedUser = (SocialLoggedUser) ((Authentication) principal)
				.getPrincipal();
		Ad ad = null;
		try {
			ad = videoApiService.setVideoDetails(adPostDto);
		} catch (VideoApiException e) {
			result.addError(new ObjectError("url", e.getMessage()));
			return "ad/register";
		}
		adPostDto.setContestId(contestId);
		adService.register(adPostDto, ad, socialLoggedUser.getId(), false);
		videoApiService.setApiData(ad);
		redirectAttributes.addFlashAttribute("info",
				"Życzymy powodzenia w konkursie");
		return "redirect:/info-page";
	}

	@RequestMapping(value = "contest/{contestId}/ad", method = RequestMethod.GET)
	public String getContestAds(
			@PathVariable("contestId") Long contestId,
			Model model,
			@RequestParam(required = false, value = "page", defaultValue = "1") int page,
			Principal principal, HttpServletRequest request) {
		AdBrowserWrapper adBrowserWrapper = adService.getContestAds(contestId,
				controllerHelperBean.getFrom(ControllerHelperBean.AD_PER_PAGE,
						page), ControllerHelperBean.AD_PER_PAGE,
				controllerHelperBean.getActive(principal));
		controllerHelperBean.preparePagination(
				ControllerHelperBean.AD_PER_PAGE, model,
				adBrowserWrapper.getTotal(), page);
		model.addAttribute("adBrowserWrapper", adBrowserWrapper);
		model.addAttribute(
				"contestAdmin",
				controllerHelperBean.isContestAdmin(request, principal,
						contestId)
						|| request.isUserInRole(UserRoleDef.ROLE_ADMIN));
		model.addAttribute("contestId", contestId);
		model.addAttribute("path", "ad/main");
		return "browser";
	}

	@RequestMapping(value = "/ad", method = RequestMethod.POST)
	public String add(@Valid @ModelAttribute("adPostDto") AdPostDto adPostDto,
			BindingResult result, Principal principal,
			RedirectAttributes redirectAttributes, HttpServletRequest request) {
		SocialLoggedUser socialLoggedUser = (SocialLoggedUser) ((Authentication) principal)
				.getPrincipal();
		if (result.hasErrors()) {
			redirectAttributes
					.addFlashAttribute("info",
							"Źle wprowadzone dane. Proszę postępować zgodnie z zaleceniami");
			return "redirect:/info-page";
		}
		// DailymotionData dailymotionData = dailyService.createMedia(
		// adPostDto.getUrl(), adPostDto.getTitle());
		boolean official = false;
		if (request.isUserInRole(UserRoleDef.ROLE_COMPANY)) {
			Company company = companyService
					.getCompanyWithBrandsByUser(socialLoggedUser.getId());
			Brand mockBrand = new Brand();
			mockBrand.setId(adPostDto.getBrandId());
			if (company.getBrands().contains(mockBrand)) {
				official = true;
			} else {
				throw new AccessDeniedException(
						"Nie masz wystarczających uprawnień aby dodać reklamę do tej marki");
			}
		}
		Ad ad = null;
		try {
			ad = videoApiService.setVideoDetails(adPostDto);
		} catch (VideoApiException e) {
			result.addError(new ObjectError("url", e.getMessage()));
			return "ad/register";
		}
		adService.register(adPostDto, ad, socialLoggedUser.getId(), official);
		videoApiService.setApiData(ad);
		// if (socialLoggedUser.getType() == LoggedType.SOCIAL)
		// siteFacebookIntegrator.notifyOnAdCreated(env.getProperty("app.domain")
		// + "/ad/" + ad.getId(), ad.getTitle(), ad.getDescription());
		// TODO:
		redirectAttributes.addFlashAttribute("info", "Reklama została dodana");
		return "redirect:/info-page";
	}

	@RequestMapping(value = { "ad/main", "/" }, method = RequestMethod.GET)
	public String mainSite(
			Model model,
			@RequestParam(required = false, value = "page", defaultValue = "1") int page,
			Principal principal) {
		AdBrowserWrapper adBrowserWrapper = adService.getMain(
				controllerHelperBean.getFrom(ControllerHelperBean.AD_PER_PAGE,
						page), ControllerHelperBean.AD_PER_PAGE,
				controllerHelperBean.getActive(principal));
		controllerHelperBean.preparePagination(
				ControllerHelperBean.AD_PER_PAGE, model,
				adBrowserWrapper.getTotal(), page);
		model.addAttribute("adBrowserWrapper", adBrowserWrapper);
		model.addAttribute("path", "ad/main");
		return "browser";
	}

	@RequestMapping(value = { "ad/waiting" }, method = RequestMethod.GET)
	public String waitingSite(
			Model model,
			@RequestParam(required = false, value = "page", defaultValue = "1") int page,
			Principal principal) {
		AdBrowserWrapper adBrowserWrapper = adService.getWaiting(
				controllerHelperBean.getFrom(ControllerHelperBean.AD_PER_PAGE,
						page), ControllerHelperBean.AD_PER_PAGE,
				controllerHelperBean.getActive(principal));
		controllerHelperBean.preparePagination(
				ControllerHelperBean.AD_PER_PAGE, model,
				adBrowserWrapper.getTotal(), page);
		model.addAttribute("adBrowserWrapper", adBrowserWrapper);
		model.addAttribute("path", "ad/waiting");

		return "browser";
	}

	@RequestMapping(value = { "user/{userId}" }, method = RequestMethod.GET)
	public String getUserAds(
			@PathVariable("userId") Long userId,
			Model model,
			@RequestParam(required = false, value = "page", defaultValue = "1") int page,
			Principal principal) {
		AdBrowserWrapper adBrowserWrapper = adService.getUserAds(userId,
				controllerHelperBean.getFrom(ControllerHelperBean.AD_PER_PAGE,
						page), ControllerHelperBean.AD_PER_PAGE,
				controllerHelperBean.getActive(principal));
		controllerHelperBean.preparePagination(
				ControllerHelperBean.AD_PER_PAGE, model,
				adBrowserWrapper.getTotal(), page);
		model.addAttribute("adBrowserWrapper", adBrowserWrapper);
		return "browser";
	}

	// @RequestMapping(value = { "ad/search"}, method = RequestMethod.GET)
	// public String browserSite(Model model) {
	// AdBrowserWrapper adBrowserWrapper = adService.getWaiting(0, 20);
	// model.addAttribute("adBrowserWrapper", adBrowserWrapper);
	// model.addAttribute("adSearchDto", new AdSearchDto());
	// model.addAttribute("path", "ad/browser");
	// return "ad/browser";
	// }

	@RequestMapping(value = { "ad/search" }, method = RequestMethod.GET)
	public String search(
			@ModelAttribute("adSearchDto") AdSearchDto adSearchDto,
			Model model,
			@RequestParam(required = false, value = "page", defaultValue = "1") int page,
			Principal principal) {
		AdBrowserWrapper adBrowserWrapper = adService.get(adSearchDto,
				controllerHelperBean.getFrom(ControllerHelperBean.AD_PER_PAGE,
						page), ControllerHelperBean.AD_PER_PAGE,
				controllerHelperBean.getActive(principal));
		controllerHelperBean.preparePagination(
				ControllerHelperBean.AD_PER_PAGE, model,
				adBrowserWrapper.getTotal(), page);
		List<Tag> tags = adService.getTags();
		List<Brand> brands = brandService.get();
		model.addAttribute("adBrowserWrapper", adBrowserWrapper);
		model.addAttribute("tagsPerColumn", (int) Math.ceil(tags.size() / 3.0));
		model.addAttribute("brandsPerColumn",
				(int) Math.ceil(brands.size() / 3.0));
		model.addAttribute("tags",
				CheckboxListWrapper.getList(tags, adSearchDto.getTagList()));
		model.addAttribute("brands",
				CheckboxListWrapper.getList(brands, adSearchDto.getBrandList()));
		model.addAttribute("path", "ad/search");
		return "ad/browser";
	}

	@RequestMapping(value = "ad/tag", method = RequestMethod.GET)
	@ResponseBody
	public List<AutocompleteDto> getBrandByTerm(
			@RequestParam("term") String term) {
		return adService.getTagsByTerm(term);
	}

	@RequestMapping(value = "ad/update/{id}", method = RequestMethod.GET)
	public String getUpdatePage(Model model, Principal principal,
			@PathVariable("id") Long adId) {
		SocialLoggedUser socialLoggedUser = (SocialLoggedUser) ((Authentication) principal)
				.getPrincipal();
		if (!adService.isOwner(adId, socialLoggedUser.getId())) {
			return "/denied";
		}
		AdPostDto adDto = new AdPostDto();
		model.addAttribute("adPostDto", adDto);
		return "ad/update";
	}

	@RequestMapping(value = "ad/{id}", method = RequestMethod.GET)
	public String get(@PathVariable("id") Long id, Model model,
			Principal principal) {
		Ad ad = adService.get(id, controllerHelperBean.getActive(principal));
		model.addAttribute("ad", ad);

		return "ad";
	}

	@RequestMapping(value = "ad/rand", method = RequestMethod.GET)
	public String get(Model model, Principal principal) {
		Ad ad = adService.rand();
		model.addAttribute("ad", ad);
		model.addAttribute("path", "ad/rand");
		return "ad";
	}

	@RequestMapping(value = "ad/{id}/comment", method = RequestMethod.GET)
	public String getComments(@PathVariable(value = "id") Long id, Model model) {
		List<AdComment> comments = adService.getComments(id);
		Ad ad = new Ad();
		ad.setId(id);
		ad.setComments(comments);
		model.addAttribute("ad", ad);
		return "comments";
	}

	@RequestMapping(value = "ad/{id}/comment", method = RequestMethod.POST)
	public String postComment(@PathVariable(value = "id") Long id, Model model,
			@RequestParam(value = "message") String message,
			@RequestParam(value = "postId", required = false) Long postId,
			Principal principal) throws AjaxNotLoggedInException {
		if (principal == null) {
			throw new AjaxNotLoggedInException();
		}
		SocialLoggedUser socialLoggedUser = (SocialLoggedUser) ((Authentication) principal)
				.getPrincipal();
		adService.comment(id, socialLoggedUser.getId(), postId, message);
		List<AdComment> comments = adService.getComments(id);
		Ad ad = new Ad();
		ad.setId(id);
		ad.setComments(comments);
		model.addAttribute("ad", ad);
		return "comments";
	}

	@RequestMapping(value = "ad/{id}/vote", method = RequestMethod.POST)
	@ResponseBody
	public String vote(@PathVariable(value = "id") Long id, Model model,
			@RequestParam(value = "vote") Short vote, Principal principal)
			throws AjaxNotLoggedInException {
		if (principal == null) {
			throw new AjaxNotLoggedInException();
		}
		SocialLoggedUser socialLoggedUser = (SocialLoggedUser) ((Authentication) principal)
				.getPrincipal();
		adService.vote(id, socialLoggedUser.getId(), vote);
		return "";
	}

	@RequestMapping(value = "ad/{id}", method = RequestMethod.PUT)
	public String update(
			@Valid @ModelAttribute("adPostDto") AdPostDto adPostDto,
			Principal principal, @PathVariable("id") Long adId) {
		SocialLoggedUser socialLoggedUser = (SocialLoggedUser) ((Authentication) principal)
				.getPrincipal();
		Ad ad = adService.update(adPostDto, adId, socialLoggedUser.getId());
		if (ad == null) {
			return "/denied";
		}
		return "ad/update";
	}

	@RequestMapping(value = "ad/{id}/state", method = RequestMethod.POST)
	@ResponseBody
	@ResponseStatus(value = HttpStatus.OK)
	public void update(
			@PathVariable("id") Long adId,
			@RequestParam(value = "place", required = false) Short place,
			@RequestParam(value = "approved", required = false) Boolean approved,
			@RequestParam(value = "ageProtected", required = false) Boolean ageProtected) {
		System.out.println(place);
		if (place != null) {
			adService.changePlace(adId, Place.values()[place]);
		}
		if (approved != null) {
			adService.changeApproval(adId, approved);
		}
		if (ageProtected != null) {
			adService.changeAgeProtected(adId, ageProtected);
		}
	}

	@RequestMapping(value = "ad/{id}/inform", method = RequestMethod.POST)
	public void informAd(@PathVariable("id") Long adId,
			@RequestParam("message") String message) {
		adService.informAd(adId, message);
	}

	@RequestMapping(value = "post/{id}/inform", method = RequestMethod.POST)
	public void informPost(@PathVariable("id") Long commentId,
			@RequestParam("message") String message) {
		adService.informComment(commentId, message);
	}

	@RequestMapping(value = "ad/rating", method = RequestMethod.POST)
	@ResponseBody
	public List<Map<String, Object>> getRatings(
			@RequestParam("ids") List<Long> ids) {
		return adService.getRatings(ids);
	}

	@ExceptionHandler(AjaxNotLoggedInException.class)
	@ResponseBody
	@ResponseStatus(value = HttpStatus.FORBIDDEN)
	public String sendAjaxLoginResponse() {
		return "login";
	}

}
