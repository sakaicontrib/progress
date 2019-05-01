package org.sakaiproject.progress.impl;

import org.sakaiproject.progress.api.IProgress.ValidationResult;
import org.sakaiproject.progress.api.IProgress;
import org.sakaiproject.progress.api.IProgressValidator;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/*
 * This class only exists because progress-impl and progress-tool are loaded
 * by different classloaders at runtime, so progress-tool can't use Class.forName()
 * to load classes from progress-impl. 
 * So we had to make a class here just to bridge the gap. 
 */
public class ProgressValidator implements IProgressValidator{

	@Autowired
	private ApplicationContext appContext;
	
	@Override
	public ValidationResult isValid(ProgressSiteConfiguration potentialSiteConfig) throws ClassNotFoundException{
		Class<?> iProgressClass = Class.forName(potentialSiteConfig.getConfigType().getImplClassName());
		IProgress iProgressImpl = (IProgress) appContext.getBean(iProgressClass);
		return iProgressImpl.isValid(potentialSiteConfig);

	}

}
