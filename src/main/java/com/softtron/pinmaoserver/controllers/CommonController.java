package com.softtron.pinmaoserver.controllers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.dom4j.Element;

import com.alibaba.fastjson.JSON;
import com.softtron.pinmaoserver.annotations.Autowried;
import com.softtron.pinmaoserver.annotations.Controller;
import com.softtron.pinmaoserver.annotations.Dao;
import com.softtron.pinmaoserver.annotations.NotNull;
import com.softtron.pinmaoserver.annotations.RequestBody;
import com.softtron.pinmaoserver.annotations.RequestMapping;
import com.softtron.pinmaoserver.annotations.Result;
import com.softtron.pinmaoserver.annotations.Service;
import com.softtron.pinmaoserver.domains.TBack;
import com.softtron.pinmaoserver.proxy.MappperHandler;
import com.softtron.pinmaoserver.utils.FileUtil;
import com.softtron.pinmaoserver.utils.FilterUtil;
import com.softtron.pinmaoserver.utils.XmlUtil;

public class CommonController extends HttpServlet {
	public static Map controllerMap = new HashMap<>();
	public static Map serviceMap = new HashMap<>();
	public static Map daoMap = new HashMap<>();
	public static Map methodsMap = new HashMap<>();
	public static Map tMapperMaps = new HashMap();
	
	// 要代理的接口
	public static Set interfaceSets = new HashSet();
	
	public <T> void common(Class<T> clazz, HttpServletRequest req, HttpServletResponse resp, String method) {
		// 获取category对象
		// json->object
		HashMap map = new HashMap();
		T category = null;
		try {
			String message = null;
			String state = "200";
			// 1、服务器接收参数
			String clientOb = req.getParameter("clientOb");
			// 2、将clientOb转对象
			Object ob = JSON.parse(clientOb);
			
			// 3、判断，如果clinetOb不为空， 先将clientOb转成业务对象， 并结合hibernate validator验证字段的限制条件
			
			if (ob != null) {
				// 验证object
				ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
				Validator validator = factory.getValidator();
				category = JSON.toJavaObject((JSON) ob, clazz);
				Set<ConstraintViolation<T>> constraintViolations = validator.validate(category);
				for (ConstraintViolation con : constraintViolations) {
					message = con.getMessage();
					state = "500";
					break;
				}
			}
			// 4、将业务对象和message,state放入map中
			Object id = null;
			// 创建HashMap,用来存储message,state,ob
			map.put("message", message);
			map.put("state", state);
			map.put("ob", category);
			
			// 5、如果message为空，则没有验证错误，进行@NotNull 注解判断，判断ob对象不为空
			
			if (message == null) {
				
				// map = (HashMap) saveorupdateCategory(map,req,resp);
				Method imethod = this.getClass().getDeclaredMethod(method, Map.class, HttpServletRequest.class,
						HttpServletResponse.class);
				// 获取方法形参的注解
				Annotation[][] annotations = imethod.getParameterAnnotations();
				for (byte i = 0; i < annotations.length; i++) {
					// 拿出每一个形参的注解
					Annotation[] annotations2 = annotations[i];
					for (byte j = 0; j < annotations2.length; j++) {
						Annotation annotation = annotations2[j];
						if (annotation instanceof NotNull) {
							NotNull notNull = (NotNull) annotation;
							String notNullMessage = notNull.message();
							String notNullState = notNull.state();
							// 暂时注释，运行放开
							// String parameter = notNull.parameter();
							// if (parameter != null) {
							// // 进行ob是否为空校验
							// if (map.get(parameter) == null) {
							// map.put("message", notNullMessage);
							// map.put("state", notNullState);
							// throw new NullPointerException();
							// }
							// }
						}
					}
					
				}
				// 6、执行反射方法
				imethod.invoke(this, map, req, resp);
				
			}
		} catch (
		
		Exception e) {
			// message = "操作失败";
			// state = "500";
			if (map.get("state").equals("200")) {
				map.put("message", "操作失败");
				map.put("state", "500");
			}
			e.printStackTrace();
		}
		
		// 数据返回json {"mes":"成功/失败","ob":id,"code":"200"}
		TBack back = new TBack((String) map.get("state"), map.get("ob"), (String) map.get("message"));
		resp.setHeader("Content-Type", "text/html;charset=UTF-8");
		try (PrintWriter pw = resp.getWriter()) {
			pw.write(JSON.toJSONString(back));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void mvcinit() {
		XmlUtil.init("heinaomvc.xml");
		FileUtil.uploadFolder = XmlUtil.getValue("heinaomvc.xml", "uploadfolder");
		FileUtil.uploadurl = XmlUtil.getValue("heinaomvc.xml", "uploadurl");
		FilterUtil.filterurls = XmlUtil.getValue("heinaomvc.xml", "filterurls");
	}
	
	public void mvc()
			throws URISyntaxException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		// 根据路径进行匹配controller
		// 解析获取所有@Controller controllers
		// 加载heinaomvc.xml
		String ipackage = XmlUtil.getValue("heinaomvc.xml", "package");
		if (ipackage != null) {
			// com.softtron.pinmaoserver=>com/softtron/pinmaoserver
			URL url = this.getClass().getClassLoader().getResource(ipackage.replaceAll("\\.", "/"));
			File file = new File(url.toURI());
			// com.softron.pinmaoserver.controllers.CategoryContoller
			Set<String> packages = new HashSet<String>();
			FileUtil.reaptFile(file, packages, "class");
			// 通过反射获取所有的有@controller类
			for (String className : packages) {
				// className com.softtron.pinmaoserver.controllers.Category
				Class clazz = Class.forName(className);
				// 存储map(key:url,value:controller对象)
				/**
				 * controllerMap
				 * {/product=com.softtron.pinmaoserver.controllers.ProductContoller@7b73ee70,
				 * /category=com.softtron.pinmaoserver.controllers.CategoryController@6bdfe4a8}
				 */
				// 获取controllerMap,serviceMap,daoMap
				commonController(clazz);// 获取controller对象，key:url,value:controller对象
				
			}
			for (String className : packages) {
				// className com.softtron.pinmaoserver.controllers.Category
				Class clazz = Class.forName(className);
				// 依赖注入
				toDI(clazz);
			}
			/**
			 * methodsMap {/deletecategory.htm=public java.util.Map
			 * com.softtron.pinmaoserver.controllers.CategoryController.deleteCategory(java.lang.String,java.util.Map,javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)
			 * throws java.lang.Exception, /findallcategory.htm=public java.util.Map
			 * com.softtron.pinmaoserver.controllers.CategoryController.findAllCategory(java.util.Map,javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)
			 * throws java.lang.Exception, /saveorupdatecategory.htm=public java.util.Map
			 * com.softtron.pinmaoserver.controllers.CategoryController.saveorupdateCategory(java.util.Map,com.softtron.pinmaoserver.domains.TCategory,javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)
			 * throws java.lang.Exception}
			 */
			
		}
		
	}
	
	private void ifController(Annotation annotation, Class clazz)
			throws InstantiationException, IllegalAccessException {
		if (annotation instanceof Controller) {
			Controller controller = (Controller) annotation;
			String url = controller.url();// 获取url
			controllerMap.put(url, clazz.newInstance());// 将url作为key,将controller对象作为value
		}
	}
	
	private void ifService(Annotation annotation, Class clazz) throws InstantiationException, IllegalAccessException {
		if (annotation instanceof Service) {
			// Service service = (Service) annotation;
			serviceMap.put(clazz.getName(), clazz.newInstance());// clazz.getName()将作为key,将service对象作为value
		}
	}
	
	private void ifDao(Annotation annotation, Class clazz) throws InstantiationException, IllegalAccessException {
		if (annotation instanceof Dao) {
			// Dao dao = (Dao) annotation;
			daoMap.put(clazz.getName(), clazz.newInstance());// clazz.getName()将作为key,将Dao对象作为value
		}
	}
	
	/**
	 * 判断是否有@Controller注解
	 * 
	 * @param clazz
	 * @return
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private void commonController(Class clazz) throws InstantiationException, IllegalAccessException {
		Annotation[] annotations = clazz.getAnnotations();
		for (Annotation annotation : annotations) {
			ifController(annotation, clazz);
			ifService(annotation, clazz);
			ifDao(annotation, clazz);
		}
		// 获取controller url
		Annotation[] clazzAnnotations = clazz.getAnnotations();
		String baseUrl = null;
		for (Annotation an : clazzAnnotations) {
			if (an instanceof Controller) {
				Controller controller = (Controller) an;
				baseUrl = controller.url();
				break;
			}
		}
		// 获取class的方法
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			Annotation[] iannotations = method.getAnnotations();
			for (Annotation iannotation : iannotations) {
				if (iannotation instanceof RequestMapping) {
					RequestMapping requestMapping = (RequestMapping) iannotation;
					String url = requestMapping.url();
					methodsMap.put(baseUrl + url, method);
				}
			}
		}
		
	}
	
	/**
	 * 依赖注入
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void toDI(Class clazz) throws IllegalArgumentException, IllegalAccessException {
		Object fromObject = null;
		Annotation[] iannotations = clazz.getAnnotations();
		// 获取对象
		for (Annotation annotation : iannotations) {
			if (annotation instanceof Controller) {
				Controller controller = (Controller) annotation;
				String url = controller.url();
				fromObject = controllerMap.get(url);
			}
			if (annotation instanceof Service) {
				fromObject = serviceMap.get(clazz.getName());
			}
			if (annotation instanceof Dao) {
				fromObject = daoMap.get(clazz.getName());
			}
		}
		if (fromObject != null) {
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				Annotation[] annotations = field.getAnnotations();
				for (Annotation annotation : annotations) {
					if (annotation instanceof Autowried) {
						// 获取属性类型
						Class iclazz = field.getType();
						Object object = serviceMap.get(iclazz.getName());
						if (object == null)
							object = daoMap.get(iclazz.getName());
						// 将字段赋值，注入
						field.setAccessible(true);
						if (object != null) {
							field.set(fromObject, object);
						} else {
							// key:com.softtron.pinmaoserver.daos.ProductDao/findAllProducts
							// value:TMapper对象
							// tMapperMaps
							if (interfaceSets.contains(field.getType().getName())) {
								field.set(fromObject, Proxy.newProxyInstance(this.getClass().getClassLoader(),
										new Class<?>[] { field.getType() }, new MappperHandler()));
							}
						}
					}
				}
			}
		}
		
	}
	
	@SuppressWarnings(value = "unchecked")
	public void common2(HttpServletRequest req, HttpServletResponse resp, String path) {
		
		Map resultMap = new HashMap();// 存储返回信息
		resultMap.put("state", "200");
		resultMap.put("message", null);
		Method method = null;
		try {
			Map map = new HashMap();// 用来存储形参名称和形参值
			// 新的根据url获取方法
			// /category/saveorupdatecategory.htm
			String[] pathes = path.split("/");
			String methodPath = null;
			String controllerPath = null;
			for (String ipath : pathes) {
				if (ipath.equals("")) {
					continue;
				}
				if (ipath.indexOf(".") != -1) {
					methodPath = "/" + ipath;
				} else {
					controllerPath = "/" + ipath;
				}
				
			}
			if (controllerPath == null) {
				throw new Exception("controllerPath is null");
			}
			if (methodPath == null) {
				throw new Exception("methodPath is null");
			}
			// 获取controller对象
			Object object = controllerMap.get(controllerPath);
			method = (Method) methodsMap.get(controllerPath + methodPath);
			
			// 原始根据url获取方法
			// Method[] methods = this.getClass().getDeclaredMethods();
			//
			// for (int i = 0; i < methods.length; i++) {
			//// if(methods[i].getName().equals(methodName)) {
			//// method = methods[i];
			//// }
			// Annotation[] methodAnnotations = methods[i].getAnnotations();
			// for (Annotation an : methodAnnotations) {
			// if (an instanceof RequestMapping) {
			// RequestMapping theAn = (RequestMapping) an;
			// if (theAn.url().equals(path)) {
			// method = methods[i];
			// break;
			// }
			// }
			// }
			// }
			
			// 获取方法参数
			Parameter[] parameters = method.getParameters();
			List<Class<?>> Classes = new ArrayList<>();
			List namees = new ArrayList<>();
			Object iob = null;
			// 迭代所有形参
			for (int i = 0; i < parameters.length; i++) {
				// 获取每一个形参
				Parameter parameter = parameters[i];
				// 获取每一个形参注解
				Annotation[] annotations = parameter.getAnnotations();
				// 获取形参类型
				Class iclass = parameter.getType();
				Classes.add(iclass);
				String iName = parameter.getName();
				namees.add(iName);
				for (int j = 0; j < annotations.length; j++) {
					
					// 获取每一个注解
					Annotation annotation = annotations[j];
					// 有@RequestBody的形参
					if (annotation instanceof RequestBody) {
						// 封装对象
						// 1、服务器接收参数(根据形参名称)
						String clientOb = req.getParameter(iName);
						// 2中文转乱码// 2、将clientOb转对象
						iob = JSON.parse(clientOb);
						if (clientOb != null) {
							String str = new String(clientOb.getBytes("ISO-8859-1"), "UTF-8");
							iob = (JSON) JSON.parse(str);
						}
						// 3、获取parameter的类型
						Class obClazz = parameter.getType();
						// Type obb = (Type)ob;
						map.put(iName, JSON.toJavaObject((JSON) iob, obClazz));
					} else if (iclass == String.class) {
						// 1、服务器接收参数(根据形参名称)
						String clientOb = req.getParameter(iName);
						map.put(iName, clientOb);
					}
					// 有@NotNull的形参
					if (annotation instanceof NotNull) {
						NotNull notNull = (NotNull) annotation;
						// 对象为空
						if (iclass == String.class && (map.get(parameter.getName()) == null
								|| ((String) map.get(parameter.getName())).trim().equals(""))) {
							resultMap.put("message", notNull.message());
							resultMap.put("state", notNull.state());
						}
						if (iclass != String.class && (map.get(parameter.getName()) == null)) {
							resultMap.put("message", notNull.message());
							resultMap.put("state", notNull.state());
							if (resultMap.get("message") != null)
								throw new Exception();
						}
						// 进入对象属性进行校验
						ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
						Validator validator = factory.getValidator();
						Object ob = map.get(parameter.getName());
						resultMap = valid(resultMap, iclass, ob, validator);
						if (resultMap.get("message") != null)
							throw new Exception();
						//
					}
					if (annotation instanceof Result) {
						map.put(iName, resultMap);
					}
				}
				if (iclass == HttpServletRequest.class) {
					map.put(iName, req);
				} else if (iclass == HttpServletResponse.class) {
					map.put(iName, resp);
				}
				
			}
			// 执行
			// 集合转数组
			Class<?>[] newclasses = new Class<?>[Classes.size()];
			newclasses = Classes.toArray(newclasses);
			Object[] obejcts = new Object[Classes.size()];
			for (int i = 0; i < namees.size(); i++) {
				obejcts[i] = map.get(namees.get(i));
			}
			// 执行反射获取的controller对象object
			resultMap = (Map) method.invoke(object, obejcts);
			
		} catch (Exception e) {
			if (resultMap.get("state").equals("200")) {
				if (e.getCause().getMessage() != null) {
					resultMap.put("message", e.getCause().getMessage());
				} else {
					resultMap.put("message", "操作失败");
				}
				
				resultMap.put("state", "500");
			}
			e.printStackTrace();
		}
		// 数据返回json {"mes":"成功/失败","ob":id,"code":"200"}
		TBack back = new TBack((String) resultMap.get("state"), resultMap.get("ob"), (String) resultMap.get("message"));
		// resp.setHeader("Content-Type", "text/html;charset=UTF-8");
		resp.setContentType("application/json; charset=utf-8");
		try (PrintWriter pw = resp.getWriter()) {
			pw.write(JSON.toJSONString(back));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private <T> Map valid(Map resultMap, Class<T> clazz, T ob, Validator validator) {
		Set<ConstraintViolation<T>> constraintViolations = validator.validate(ob);
		for (ConstraintViolation con : constraintViolations) {
			resultMap.put("message", con.getMessage());
			resultMap.put("state", "500");
			break;
		}
		return resultMap;
	}
	
	public void mapper() throws Exception {
		// 1获取xml，2解析xml,3封装xml对象
		// 获取到文件夹
		String path = XmlUtil.getValue("heinaomvc.xml", "mapper");
		System.out.println(path);
		// 存xml文件名称
		Set<String> packages = new HashSet<String>();
		// daos文件夹
		File file = FileUtil.getFile(path);
		// 找xml文件
		FileUtil.reaptFile(file, packages, "xml");
		
		// packages[com.softtron.pinmaoserver.daos.productDao.xml]
		for (String xmlFile : packages) {
			String ipath = xmlFile.replaceAll("\\.(?!(xml))", "\\\\");
			XmlUtil.init(ipath);// 初始化文件,放入map中
			// 获取namespacename
			String interfacename = XmlUtil.getAttribute(ipath, "namespace");
			Map tMapperMap = XmlUtil.getMapperClass(interfacename, (Element) XmlUtil.elementMap.get(ipath));
			tMapperMaps.putAll(tMapperMap);
		}
		System.out.println(tMapperMaps);
		
	}
}
