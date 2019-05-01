package org.sakaiproject.progress.api;

import org.sakaiproject.progress.api.IProgress.ValidationResult;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;

public interface IProgressValidator {

	ValidationResult isValid(ProgressSiteConfiguration potentialSiteConfig) throws ClassNotFoundException;
}
