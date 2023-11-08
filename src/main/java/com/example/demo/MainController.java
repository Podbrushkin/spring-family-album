package com.example.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

import com.example.demo.data.ImageRepositoryNeo4j;
import com.example.demo.model.PersonDto;
import com.example.demo.model.Tag;
import com.example.demo.service.ImageService;
import com.example.demo.service.PersonService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@SessionAttributes({"selectedTags","selectedYear","currentPage","pageSize","images"})
public class MainController {
	Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	ImageProvider imageProvider;
	@Autowired
	Catalog catalog;
	@Autowired
	ImageService imageService;
	@Autowired
	PersonService personService;

	@ModelAttribute("allDepictedPeople")
	public Collection<PersonDto> addAllDepictedPeople() {
		return personService.findAllDepictedWithCountsDto();
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

	@GetMapping(value = "/processForm")
	public String processForm(
			@RequestParam(required = false) List<String> tag,
			@RequestParam Optional<Integer> selectedYear,
			ModelMap model) {
		
		if (tag == null) {tag = List.of();}
		log.trace("RequestParams: tags={}, year={}", tag, selectedYear);
		model.put("selectedTags", tag);
		model.put("selectedYear", selectedYear);
		return "redirect:/search";
	}

	@GetMapping(value = "/search")
	public String processQueryParams(
			@SessionAttribute(required = false) List<String> selectedTags,
			@SessionAttribute(required = false) Optional<Integer> selectedYear,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "pageSize", defaultValue = "40") int pageSize,
			@RequestParam(name = "goBack", defaultValue = "false") boolean goBack,
			ModelMap model,
			HttpSession session) {
		log.trace("SessionAttrs: tags={}, year={}", selectedTags, selectedYear);

		if (selectedTags == null) {
			return "redirect:/";
		} else if (goBack) {
			page = (int) session.getAttribute("currentPage");
			pageSize = (int) session.getAttribute("pageSize");
		}
		
		var imagePage = imageService.getImagesForTagsPageable(selectedTags, selectedYear, page, pageSize);
		model.put("images", imagePage.getItems());
		model.addAttribute("currentPage", page);
		model.addAttribute("pageSize", pageSize);
		model.addAttribute("totalPages", imagePage.getTotalPages());
		model.addAttribute("totalImages", imagePage.getTotalItems());
		
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
		var imgObj = imageService.getImageForHash(imgHash);
		byte[] image = imageProvider.getThumbnailBytes(imgObj);

		response.setContentLength(image.length);
		var os = response.getOutputStream();

		os.write(image, 0, image.length);
	}

	@GetMapping("/imagePage/{imgHash:[^\\\\.]+}")
	public String getImagePageByHash(@PathVariable(required = true) String imgHash,
			@SessionAttribute(required = false) List<String> selectedTags,
			@SessionAttribute Optional<Integer> selectedYear,
			ModelMap model) {
		var img = imageService.getImageForHash(imgHash);
		model.addAttribute("image", img);
		if (selectedTags != null) {
		var prevAndNext = imageService.getNextAndPreviousImages(selectedTags, selectedYear, img);
			model.put("previous", prevAndNext.get("previous"));
			model.put("next", prevAndNext.get("next"));
		}
		return "image.html";
	}

	



	@GetMapping(value = "/showGraph")
	public String graph() {
		return "graph.html";
	}
	@GetMapping(value = "/showTable")
	public String table(ModelMap model) {
		var people = personService.findAll();
		// personRepository.findAll();
        List<Map> namesBdays = people.stream()
            .map(p -> Map.of("fullName",p.getFullName(),
                "birthday",p.getBirthday() == null ? "" : p.getBirthday()))
            .collect(Collectors.toList());
		model.put("people", namesBdays);
		return "table.html";
	}
	@GetMapping(value = "/showFamilyTree")
	public String tree(ModelMap model) {
		var predecessor = personService.findOneWithMostAncestors();
		log.trace("Ancestors of this person will be shown: {}({} children)", predecessor.getFullName(),predecessor.getChildren().size());
		var mermaid = 
			personService.getMermaidGraphFor(predecessor);
		
		model.put("mermaid", mermaid);

		var graphviz = personService.getFullGraphvizSvg();
		model.put("svg", graphviz);
		return "mermaidGraph.html";
	}

	@Autowired
	ImageRepositoryNeo4j imageRep;

	@GetMapping(value =  "/findImagesByCypherQuery")
	public String findImagesByCypherQuery(
		@RequestParam String cypherQuery, 
		ModelMap model) {
		log.debug("Got this cypherQuery: {}", cypherQuery);
		// var images = imageRep.findImagesByCypherQuery(cypherQuery);
		var images = imageRep.findImagesByCypherQuery(cypherQuery);
		
		model.put("cypherQuery", cypherQuery);
		model.put("images", images);
		model.put("pageSize", 25);
		model.put("currentPage", 1);
		
		return "index.html";
	}
	
	@GetMapping(value =  "/")
	public String cypherGoToPage(
		@RequestParam(defaultValue = "1") Integer page,
		ModelMap model
	) {
		model.put("currentPage", page);
		return "index.html";
	}


}