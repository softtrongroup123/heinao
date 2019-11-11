package com.softtron.pinmaoserver.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.softtron.pinmaoserver.controllers.CommonController;
import com.softtron.pinmaoserver.domains.TMapper;
import com.softtron.pinmaoserver.domains.TMapperType;

public class XmlUtil {
	public static Map elementMap = new HashMap<>();// 存储节点
	
	// 存储key:文件名 value:root
	public static void init(String fileName) {
		try {
			// 读取xml文件
			File file = new File(XmlUtil.class.getClassLoader().getResource(fileName).toURI());
			SAXReader xmlReader = new SAXReader();
			// 获取document
			Document xml = xmlReader.read(file);
			// 进行解析
			Element root = xml.getRootElement();// 获取到xml根节点
			elementMap.put(fileName, root);
			
		} catch (URISyntaxException | DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 递归
	 * 
	 * @param element
	 * @param key
	 * @return
	 */
	public static Element repeatFindText(Element element, String key) {
		Iterator elementIterator = element.elementIterator();
		while (elementIterator.hasNext()) {
			Element sonElement = (Element) elementIterator.next();
			if (sonElement.getName().equals(key)) {// 获取名字
				return sonElement;
			} else {
				repeatFindText(sonElement, key);
			}
		}
		return null;
	}
	
	public static String repeatFindAttribute(Element element, String key) {
		String value = null;
		value = element.attribute("namespace").getValue();
		return value;
	}
	
	/**
	 * 根据文件名和属性名获取属性名所对应的的值
	 * 
	 * @param fileName文件名
	 * @param key属性名
	 * @return
	 */
	public static String getValue(String fileName, String key) {
		Element root = (Element) elementMap.get(fileName);
		Element element = repeatFindText(root, key);
		if (element != null)
			return element.getText();// 获取内容
		return null;
	}
	
	public static String getAttribute(String fileName, String key) throws Exception {
		Element root = (Element) elementMap.get(fileName);
		String value = repeatFindAttribute(root, key);
		if (value == null) {
			throw new Exception(fileName + " namespace is null");
		}
		return value;
	}
	
	/**
	 * 读取mapper xml中的信息，将信息封装成TMapperType element是文件的根节点
	 * 
	 * @throws ClassNotFoundException
	 */
	public static Map getMapperClass(String interfacename, Element element) throws ClassNotFoundException {
		Iterator elementIterator = element.elementIterator();
		Map map = new HashMap();
		while (elementIterator.hasNext()) {
			Element sonElement = (Element) elementIterator.next();
			TMapperType type = TMapperType.valueOf(sonElement.getName().toUpperCase());
			String sql = sonElement.getText();
			String id = sonElement.attribute("id").getValue();
			Attribute attr = sonElement.attribute("resultType");
			Class resultType = null;
			if (attr != null) {
				resultType = Class.forName(attr.getValue());
			}
			Class parameterType = Class.forName(sonElement.attribute("parameterType").getValue());
			// com.softtron.pinmaoserver.daos.ProductDao/findAllProducts
			// 存储要代理的接口
			CommonController.interfaceSets.add(interfacename);
			map.put(interfacename + "/" + id, new TMapper(id, resultType, parameterType, type, sql));
		}
		return map;
	}
	
}
