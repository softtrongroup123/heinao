package com.softtron.pinmaoserver.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

public class FileUtil {
	static ClassLoader classLoader = FileUtil.class.getClassLoader();
	public static String uploadFolder;
	public static String uploadurl;
	
	/**
	 * 获取所有class文件
	 * 
	 * @param file
	 * @param packages
	 *                     type 扫描的文件类型
	 */
	public static void reaptFile(File file, Set packages, String type) {
		if (file!=null&&file.isDirectory()) {
			// 获取文件夹中的文件
			File[] sonFiles = file.listFiles();
			for (File sonFile : sonFiles) {
				if (sonFile.isFile()) {
					// System.out.println(sonFile.getName());
					String path = sonFile.getPath();
					// classes/com/sotrron/.../RouterController.class
					if (type.equals("class") && type.equals(path.substring(path.lastIndexOf(".") + 1)))
						packages.add(path.substring(path.indexOf("classes") + 8, path.lastIndexOf("."))
								.replaceAll("\\\\", "."));
					if (type.equals("xml") && type.equals(path.substring(path.lastIndexOf(".") + 1)))
						packages.add(path.substring(path.indexOf("classes") + 8).replaceAll("\\\\", "."));
				} else if (sonFile.isDirectory()) {
					reaptFile(sonFile, packages, type);
				}
			}
		}
	}
	
	public static File getFile(String path) throws URISyntaxException {
		URL url = FileUtil.classLoader.getResource(path);
		if (url != null) {
			return new File(url.toURI());
		} else {
			return null;
		}
	}
	
	public static Set<String> uploadFiles(HttpServletRequest request)
			throws IllegalStateException, IOException, ServletException, URISyntaxException {
		Set<String> paths = new HashSet<>();
		// 存储路径
		File folder = new File(request.getServletContext().getRealPath(FileUtil.uploadFolder));
		if (!folder.exists()) {
			folder.mkdirs();
		}
		String savePath = folder.getPath();
		// 获取上传的文件集合
		Collection<Part> parts = request.getParts();
		// 上传单个文件
		if (parts.size() == 1) {
			// Servlet.将multipart/form-data的POST请求封装成Part，通过Part对上传的文件进行操作。
			// Part part = parts[];//从上传的文件集合中获取Part对象
			Part part = request.getPart("file");// 通过表单file控件(<input type="file" name="file">)的名字直接获取Part对象
			// Servlet没有提供直接获取文件名的方法,需要从请求头中解析出来
			// 获取请求头，请求头的格式：form-data; name="file"; filename="snmpj--api.zip"
			String header = part.getHeader("content-disposition");
			// 获取文件名
			String fileName = getFileName(header);
			fileName = UUID.randomUUID().toString() + fileName.substring(fileName.indexOf("."));
			// 把文件写到指定路径
			part.write(savePath + File.separator + fileName);
			paths.add(FileUtil.uploadurl + fileName);
		} else {
			// 一次性上传多个文件
			for (Part part : parts) {// 循环处理上传的文件
				// 获取请求头，请求头的格式：form-data; name="file"; filename="snmpj--api.zip"
				String header = part.getHeader("content-disposition");
				// 获取文件名
				String fileName = getFileName(header);
				fileName = UUID.randomUUID().toString() + fileName.substring(fileName.indexOf("."));
				// 把文件写到指定路径
				part.write(savePath + File.separator + fileName);
				paths.add(FileUtil.uploadurl + fileName);
			}
		}
		return paths;
	}
	
	private static String getFileName(String header) {
		
		String fileName = header.substring(header.indexOf("filename=") + 9).replace("\"", "");
		return fileName;
	}
	
}
