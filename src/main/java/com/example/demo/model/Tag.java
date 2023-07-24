package com.example.demo.model;

public class Tag {
	private String id;
	private String name;
	private int imagesCount;

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