package com.softtron.pinmaoserver.controllers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@MultipartConfig
public class RouterController extends CommonController {
	@Override
	public void init() throws ServletException {
		try {
			super.mvcinit();//加载heinaomvc.xml
			//进行文件映射扫描
			super.mapper();
			//实现了依赖注入
			//@Controller,@Requestmapping,@RequestBody
			//@Service @Dao @Autowired @NotNull
			super.mvc();
			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String servletPath = req.getServletPath();
		common2(req, resp,servletPath);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		String servletPath = req.getServletPath();
//		switch (servletPath) {
//		case "/saveorupdatecategory.htm":
//			//common(TCategory.class,req, resp,"saveorupdateCategory");
//			common2(req, resp,"saveorupdateCategory");
//			break;
//		case "/findallcategory.htm":
//			common(Map.class,req, resp,"findAllCategory");
//			break;	
//
//		default:
//			break;
//		}
		
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println("Hello");
	}
	
}
