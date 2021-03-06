package com.somnus.action;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;

import com.somnus.model.messege.Grid;
import com.somnus.model.messege.Message;
import com.somnus.service.BaseService;
import com.somnus.support.constant.Constants;
import com.somnus.support.exception.BizException;
import com.somnus.support.exception.SysRuntimeException;
import com.somnus.support.pagination.Pageable;
import com.somnus.support.pagination.impl.PageRequest;
import com.somnus.util.base.BeanUtils;
import com.somnus.util.base.FastjsonFilter;
import com.somnus.util.base.HqlFilter;
import com.somnus.util.base.MessageUtil;
import com.somnus.util.base.MsgCodeList;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.opensymphony.xwork2.ActionSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.web.util.WebUtils;
/**
 * 基础ACTION,其他ACTION继承此ACTION来获得writeJson和ActionSupport的功能
 * 
 * 基本的CRUD已实现，子类继承BaseAction的时候，提供setService方法即可
 * 
 * 注解@Action后，访问地址就是命名空间+类名(全小写，并且不包括Action后缀)，本action的访问地址就是/base.sy
 * 
 * @author Somnus
 * 
 */
@ParentPackage("Package")
@Namespace("/")
@Action
public class BaseAction<T> extends ActionSupport {
	
	private static final long serialVersionUID = -5039657025856216857L;

	private transient Logger	log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private MessageSourceAccessor msa;

	protected String captcha;

	protected String id;// 主键
	protected String ids;// 主键集合，逗号分割
	protected T data;// 数据模型(与前台表单name相同，name="data.xxx")

	protected BaseService<T> service;// 业务逻辑
	
	protected String entity;
	protected String name;
	protected String value;
	/**
	 * 继承BaseAction的action需要先设置这个方法，使其获得当前action的业务服务
	 * 
	 * @param service
	 */
	public void setService(BaseService<T> service) {
		this.service = service;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIds() {
		return ids;
	}

	public void setIds(String ids) {
		this.ids = ids;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		this.entity = entity;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	

	/**
	 * 将对象转换成JSON字符串，并响应回前台
	 * 
	 * @param object
	 * @param includesProperties
	 *            需要转换的属性
	 * @param excludesProperties
	 *            不需要转换的属性
	 */
	public void writeJsonByFilter(Object object, String[] includesProperties, String[] excludesProperties) {
		try {
			FastjsonFilter filter = new FastjsonFilter();// excludes优先于includes
			if (excludesProperties != null && excludesProperties.length > 0) {
				filter.getExcludes().addAll(Arrays.<String> asList(excludesProperties));
			}
			if (includesProperties != null && includesProperties.length > 0) {
				filter.getIncludes().addAll(Arrays.<String> asList(includesProperties));
			}
			log.info("对象转JSON：要排除的属性[" + excludesProperties + "]要包含的属性[" + includesProperties + "]");
			String json;
			String User_Agent = getRequest().getHeader("User-Agent");
			if (StringUtils.indexOfIgnoreCase(User_Agent, "MSIE 6") > -1) {
				// 使用SerializerFeature.BrowserCompatible特性会把所有的中文都会序列化为\\uXXXX这种格式，字节数会多一些，但是能兼容IE6
				json = JSON.toJSONString(object, filter, SerializerFeature.WriteDateUseDateFormat, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.BrowserCompatible);
			} else {
				// 使用SerializerFeature.WriteDateUseDateFormat特性来序列化日期格式的类型为yyyy-MM-dd hh24:mi:ss
				// 使用SerializerFeature.DisableCircularReferenceDetect特性关闭引用检测和生成
				json = JSON.toJSONString(object, filter, SerializerFeature.WriteDateUseDateFormat, SerializerFeature.DisableCircularReferenceDetect);
			}
			log.info("转换后的JSON字符串：" + json);
			getResponse().setContentType("text/html;charset=utf-8");
			getResponse().getWriter().write(json);
			getResponse().getWriter().flush();
			getResponse().getWriter().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将对象转换成JSON字符串，并响应回前台
	 * 
	 * @param object
	 * @throws IOException
	 */
	public void writeJson(Object object) {
		writeJsonByFilter(object, null, null);
	}

	/**
	 * 将对象转换成JSON字符串，并响应回前台
	 * 
	 * @param object
	 * @param includesProperties
	 *            需要转换的属性
	 */
	public void writeJsonByIncludesProperties(Object object, String[] includesProperties) {
		writeJsonByFilter(object, includesProperties, null);
	}

	/**
	 * 将对象转换成JSON字符串，并响应回前台
	 * 
	 * @param object
	 * @param excludesProperties
	 *            不需要转换的属性
	 */
	public void writeJsonByExcludesProperties(Object object, String[] excludesProperties) {
		writeJsonByFilter(object, null, excludesProperties);
	}

	/**
	 * 获得request
	 * 
	 * @return
	 */
	public HttpServletRequest getRequest() {
		return ServletActionContext.getRequest();
	}

	/**
	 * 获得response
	 * 
	 * @return
	 */
	public HttpServletResponse getResponse() {
		return ServletActionContext.getResponse();
	}

	/**
	 * 获得session
	 * 
	 * @return
	 */
	public HttpSession getSession() {
		return ServletActionContext.getRequest().getSession();
	}

	/**
	 * 获得一个对象
	 */
	public void getById() {
		Message message = new Message();
		try {
			if (!StringUtils.isBlank(id)) {
				MessageUtil.createCommMsg(message);
				message.setData(service.getById(id));
			} else {
				throw new BizException(msa.getMessage(MsgCodeList.ERROR_300003));
			}
		} catch (BizException e) {
			log.error(Constants.BUSINESS_ERROR, e);
			// 组织错误报文
			MessageUtil.errRetrunInAction(message, e);
		} catch (Exception ex) {
			log.error(Constants.EXCEPTION_ERROR, ex);
			// 组织错误报文
			MessageUtil.createErrorMsg(message);
		}
		writeJson(message);
	}

	/**
	 * 查找一批对象
	 */
	public void find() {
		HqlFilter hqlFilter = new HqlFilter(getRequest());
		Pageable pageable = null;
		if(getRequest().getParameter("pageSize") == null){
			Integer start = findIntegerParameterValue(getRequest(), Constants.PAGE_PARAM_START);
			pageable = new PageRequest(start == null ? 1 : start,Constants.DEFAULT_LIMIT);
		} else {
			pageable = this.findPage(getRequest());
		}
		Pageable result = service.findByFilter(hqlFilter, pageable);
		writeJson(result.getResult(List.class));
	}

	/**
	 * 查找所有对象
	 */
	public void findAll() {
		HqlFilter hqlFilter = new HqlFilter(getRequest());
		writeJson(service.findByFilter(hqlFilter));
	}

	/**
	 * 查找分页后的grid
	 */
	public void grid() {
		Grid grid = new Grid();
		HqlFilter hqlFilter = new HqlFilter(getRequest());
		Pageable pageable = null;
		if(getRequest().getParameter("pageSize") == null){
			Integer start = findIntegerParameterValue(getRequest(), Constants.PAGE_PARAM_START);
			pageable = new PageRequest(start == null ? 1 : start,Constants.DEFAULT_LIMIT);
		} else {
			pageable = this.findPage(getRequest());
		}
		Pageable result = service.findByFilter(hqlFilter, pageable);
		grid.setTotal(result.getCount());
		grid.setRows(result.getResult(List.class));
		writeJson(grid);
	}

	/**
	 * 查找grid所有数据，不分页
	 */
	public void gridAll() {
		Grid grid = new Grid();
		HqlFilter hqlFilter = new HqlFilter(getRequest());
		List<T> l = service.findByFilter(hqlFilter);
		grid.setTotal(l.size());
		grid.setRows(l);
		writeJson(grid);
	}

	/**
	 * 获得treeGrid，treeGrid由于提供了pid的扩展，所以不分页
	 */
	public void treeGrid() {
		HqlFilter hqlFilter = new HqlFilter(getRequest());
		writeJson(service.findByFilter(hqlFilter));
	}

	/**
	 * 保存一个对象
	 */
	public void save() {
		Message message = new Message();
		if (data != null) {
			service.save(data);
			MessageUtil.createCommMsg(message);
		}
		writeJson(message);
	}

	/**
	 * 更新一个对象
	 */
	public void update() {
		Message message = new Message();
		String reflectId = null;
		try {
			if (data != null) {
				reflectId = (String) FieldUtils.readField(data, "id", true);
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		if (!StringUtils.isBlank(reflectId)) {
			T t = service.getById(reflectId);
			BeanUtils.copyNotNullProperties(data, t, new String[] { "createdatetime" });
			service.update(t);
			MessageUtil.createCommMsg(message);
		}
		writeJson(message);
	}

	/**
	 * 删除一个对象
	 */
	public void delete() {
		Message message = new Message();
		if (!StringUtils.isBlank(id)) {
			T t = service.getById(id);
			service.delete(t);
			MessageUtil.createCommMsg(message);
		}
		writeJson(message);
	}
	public void checkIsUnique(){
		Message json = new Message();
		log.info(entity+" " + name+" "+value);
		json.setSuccess(true);
		if(value.equals("admin"))
			json.setUnique(false);
		writeJson(json);
	}
	
	/**
	 * @Description 获取分页信息
	 * @param request
	 * @return
	 */
	protected Pageable findPage(HttpServletRequest request){		
		return findPage(request, Constants.PAGE_PARAM_START, Constants.PAGE_PARAM_LIMIT);
	}

	/**
	 * @Description 获取分页信息
	 * @param request
	 * @param pageFieldName 起始页字段名称
	 * @param pageSizeFieldName 单页总量字段名称
	 * @return
	 */
	protected Pageable findPage(HttpServletRequest request, String pageFieldName, String pageSizeFieldName){
		Validate.notBlank(pageFieldName, "page field name required");
		Validate.notBlank(pageSizeFieldName, "pageSize field name required");
		Integer start = findIntegerParameterValue(request, pageFieldName);
		Integer limit = findIntegerParameterValue(request, pageSizeFieldName);
		if(limit == null){
			throw new SysRuntimeException("pageSize is required");
		}
		//限制pageSize <= 100
		if(limit > 100){
			log.warn("pageSize must be less than 100");
			limit = 100;
		};
		return new PageRequest(
				start == null ? 1 : start,
				limit	
		);
	}
	
	/**
	 * @Description 从请求中获取Integer类型参数
	 * @param request
	 * @param name 参数名称
	 * @return
	 */
	protected Integer findIntegerParameterValue(HttpServletRequest request,
			String name) {
		String pv = WebUtils.findParameterValue(request, name);
		return StringUtils.isBlank(pv) ? null : Integer.parseInt(pv);
	}

	/**
	 * @Description 从请求中获取Long类型参数
	 * @param request
	 * @param name 参数名称
	 * @return
	 */
	protected Long findLongParameterValue(HttpServletRequest request,
			String name) {
		String pv = WebUtils.findParameterValue(request, name);
		return StringUtils.isBlank(pv) ? null : Long.parseLong(pv);
	}
	
	/**
	 * @Description 从请求中获取BigDecimal类型参数
	 * @param request
	 * @param name 参数名称
	 * @return
	 */
	protected BigDecimal findBigDecimalParameterValue(HttpServletRequest request,
			String name) {
		String pv = WebUtils.findParameterValue(request, name);
		return StringUtils.isBlank(pv) ? null : new BigDecimal(pv);
	}

	/**
	 * @Description 从请求中获取String类型参数
	 * @param request
	 * @param name 参数名称
	 * @return
	 */
	protected String findStringParameterValue(HttpServletRequest request,
			String name) {
		return WebUtils.findParameterValue(request, name);
	}

	/**
	 * @Description 从请求中获取Boolean类型参数
	 * @param request
	 * @param name 参数名称
	 * @return
	 */
	protected Boolean findBooleanParameterValue(HttpServletRequest request,
			String name) {
		String pv = WebUtils.findParameterValue(request, name);
		return StringUtils.isBlank(pv) ? null : Boolean.parseBoolean(pv);
	}

	/**
	 * @Description 从请求中获取Date类型参数
	 * @param request
	 * @param name 参数名称
	 * @param datePattern 日期模式
	 * @return
	 * @throws ParseException
	 */
	protected Date findDateParameterValue(HttpServletRequest request,
			String name, String datePattern) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat(datePattern);
		String pv = WebUtils.findParameterValue(request, name);
		return StringUtils.isBlank(pv) ? null : dateFormat.parse(WebUtils.findParameterValue(request, name));
	}

}
