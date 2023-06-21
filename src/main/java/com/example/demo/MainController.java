package com.example.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@SessionAttributes("checkboxForm")
public class MainController {
	Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	ImageProvider imageProvider;
	@Autowired
	Catalog catalog;
	@Autowired
	ImageService imageService;

	@ModelAttribute("selectableTags")
	public List<Tag> addSelectableTags() {
		List<Tag> tags = new ArrayList<Tag>();
		catalog.getTags().forEach(s -> {
			var imagePage = imageService.getImagesForTags(List.of(s), Optional.empty(), 1, 1);
			long imagesCount = imagePage.getTotalItems();
			tags.add(new Tag(s, catalog.getTagNameExtended(s), imagesCount));
		});
		tags.sort(Comparator.comparing(t -> t.getName()));
		return tags;
	}

	@ModelAttribute("selectableYears")
	public List<Tag> addSelectableYears() {
		List<Tag> selectableYears = new ArrayList<>();
		Map<Integer, Long> yearToImageCountMap = catalog.getNumberOfPhotosByYear();
		for (int year : yearToImageCountMap.keySet()) {
			selectableYears.add(new Tag(year + "", null, yearToImageCountMap.get(year)));
		}
		selectableYears.sort(Comparator.comparing(t -> t.getName()));
		return selectableYears;
	}

	@ModelAttribute("checkboxForm")
	public CheckboxForm addCheckboxForm() {
		return new CheckboxForm();
	}

	 @GetMapping(value="/")
	public String index(ModelMap model) {

		// model.put("checkboxForm", new CheckboxForm());
		log.info("index() was called");
		return "index.html";
	}

	@GetMapping(value="/", params="selectedYear")
	public String processForm(@ModelAttribute("checkboxForm") CheckboxForm form, ModelMap model,
			HttpSession session) {
		log.info("processform() was called");
		List<String> selectedTags = form.getCheckboxList();
		String selectedYearStr = form.getSelectedYear();
		Integer selectedYear = selectedYearStr.isBlank() ? null : Integer.parseInt(selectedYearStr);

		Optional<Integer> selectedYearOpt = Optional.ofNullable(selectedYear);
		session.setAttribute("selectedTags", selectedTags);
		session.setAttribute("selectedYear", selectedYearOpt);

		model.remove("totalPages");
		model.remove("currentPage");
		model.remove("pageSize");
		//
		return "redirect:/listImages";
	}

	@GetMapping("/listImages")
	public String listImages(
			ModelMap model,
			@SessionAttribute List<String> selectedTags,
			@SessionAttribute Optional<Integer> selectedYear,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "pageSize", defaultValue = "40") int pageSize,
			HttpSession session) {

		log.info("Selected size: " + selectedTags.size());
		log.info("Selected: " + selectedTags);
		log.info("Selected year: " + selectedYear);
		
		var imagePage = imageService.getImagesForTags(selectedTags, selectedYear, page, pageSize);
		model.put("images", imagePage.getItems());

		// Pass the current page, page size, and total number of images to the template
		// int totalImages = imageService.countImagesForTags(selectedTags);
		// int totalPages = (int) Math.ceil(totalImages / (double) pageSize);
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("totalPages", imagePage.getTotalPages());
		model.addAttribute("totalImages", imagePage.getTotalItems());
		// session.removeAttribute("selectedTags");

		return "index.html";
	}

	@RequestMapping(value = "/image/{imgHash}", method = RequestMethod.GET)
	public @ResponseBody void getImageByHash(@PathVariable(required = true) String imgHash,
			HttpServletResponse response, HttpServletRequest request) throws IOException, NullPointerException {

		response.setContentType("image/jpeg");

		byte[] image = imageProvider.getImageBytes(imgHash);

		response.setContentLength(image.length);
		var os = response.getOutputStream();

		os.write(image, 0, image.length);
	}
	@RequestMapping(value = "/thumbnail/{imgHash}", method = RequestMethod.GET)
	public @ResponseBody void getThumbnailByHash(@PathVariable(required = true) String imgHash,
			HttpServletResponse response, HttpServletRequest request) throws IOException, NullPointerException {

		response.setContentType("image/jpeg");
		var imgObj = catalog.getImageForMgckHash(imgHash);
		byte[] image = imageProvider.getThumbnailBytes(imgObj);

		response.setContentLength(image.length);
		var os = response.getOutputStream();

		os.write(image, 0, image.length);
	}

	class Tag {
		String id;
		String name;
		int imagesCount;

		public Tag(String id) {
			this.id = id;
		}

		public Tag(String id, String name, Long imagesCount) {
			this.id = id;
			if (name != null) {
				this.name = name;
			} else {
				this.name = id;
			}
			this.imagesCount = imagesCount.intValue();
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setImagesCount(int imagesCount) {
			this.imagesCount = imagesCount;
		}

		public int getImagesCount() {
			return imagesCount;
		}

	}

	public class CheckboxForm {
		private List<String> checkboxList;
		private String selectedYear;

		public String getSelectedYear() {
			return selectedYear;
		}

		public void setSelectedYear(String selectedYear) {
			this.selectedYear = selectedYear;
		}

		public List<String> getCheckboxList() {
			return checkboxList;
		}

		public void setCheckboxList(List<String> checkboxList) {
			this.checkboxList = checkboxList;
		}

		@Override
		public String toString() {
			return "CheckboxForm [checkboxList=" + checkboxList + "]";
		}

		// getters and setters
	}
}