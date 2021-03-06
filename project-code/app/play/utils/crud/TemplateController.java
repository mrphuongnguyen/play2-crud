package play.utils.crud;

import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

import play.Logger;
import play.Logger.ALogger;
import play.twirl.api.Html;
import play.twirl.api.Content;
import play.mvc.Controller;
import play.mvc.Result;

public class TemplateController extends Controller {

	protected final ALogger log = Logger.of(getClass());
	
	private ClassLoader classLoader;
	protected String templatePackageName = "views.html."; //derived classes can change this by reassigning it in a constructor. MUST end in "."

	public TemplateController(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public TemplateController() {
	}

	protected Result ok(String template, Parameters params) {
		return ok(render(template, params));
	}

	protected Result badRequest(String template, Parameters params) {
		return badRequest(render(template, params));
	}
	
	protected Content render(String template, Parameters params) {
		Content content;
		try {
			content = call(templatePackageName + template, "render", params);
		} catch (ClassNotFoundException | MethodNotFoundException e) {
			if (log.isDebugEnabled())
				log.debug("template not found : '" + template + "'");
			return templateNotFound(template, params);
		}
		return content;
	}

	protected Content templateNotFound(String template, Parameters params) {
		StringBuilder sb = new StringBuilder("Template ");
		sb.append(templatePackageName).append(template).append("(");
		Class<?>[] types = params.types();
		for (int i = 0; i < types.length; i++) {
			Class<?> type = types[i];
			if (i != 0)
				sb.append(", ");
			sb.append(type.getSimpleName());
		}
		sb.append(") is not found");
		return Html.apply(sb.toString());
	}

	protected <T> Parameters with() {
		return new Parameters();
	}

	protected <T> Parameters with(Class<?> paramType, T paramValue) {
		return new Parameters(paramType, paramValue);
	}

	@SuppressWarnings("unchecked")
	protected <R> R call(String className, String methodName, Parameters params)
			throws ClassNotFoundException, MethodNotFoundException {
		if (log.isDebugEnabled())
			log.debug("call <-");
		if (log.isDebugEnabled())
			log.debug("className : " + className);
		if (log.isDebugEnabled())
			log.debug("methodName : " + methodName);
		if (log.isDebugEnabled())
			log.debug("params : " + params);
		
		ClassLoader cl = classLoader();
		Object result = null;
		Class<?> clazz = cl.loadClass(className);
		if (log.isDebugEnabled())
			log.debug("clazz : " + clazz);

		Class<?>[] paramTypes = params.types();
		Object[] paramValues = params.values();
		Method method = ReflectionUtils.findMethod(clazz, methodName,
				paramTypes);
		if (log.isDebugEnabled())
			log.debug("method : " + method);

		if (method == null) {
			throw new MethodNotFoundException(className, methodName, paramTypes);
		}
		result = ReflectionUtils.invokeMethod(method, null, paramValues);
		return (R) result;
	}

	private ClassLoader classLoader() {
		//create class loader if one does not exist
		if (classLoader == null) {
			classLoader = getClass().getClassLoader();
		}
		return classLoader;
	}

	public class MethodNotFoundException extends Exception {

		private static final long serialVersionUID = 1L;
		private String className;
		private String methodName;
		private Class<?>[] paramTypes;

		public MethodNotFoundException(String className, String methodName,
				Class<?>[] paramTypes) {
			this.className = className;
			this.methodName = methodName;
			this.paramTypes = paramTypes;
		}

		public String toString() {
			StringBuffer sb = new StringBuffer(className);
			sb.append(".").append(methodName).append("(");
			for (int i = 0; i < paramTypes.length; i++) {
				Class<?> t = paramTypes[i];
				sb.append(t.getSimpleName());
				if (i < paramTypes.length - 1)
					sb.append(", ");
			}
			sb.append(")");
			return sb.toString();
		}

	}
}
