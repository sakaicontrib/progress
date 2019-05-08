package org.sakaiproject.progress.impl;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.Message;
import org.sakaiproject.api.app.messageforums.Topic;
import org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager;
import org.sakaiproject.progress.api.IProgress;
import org.sakaiproject.progress.api.ProgressRole;
import org.sakaiproject.progress.api.ProgressServiceException;
import org.sakaiproject.progress.model.data.entity.ProgressAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressConfigurationAttributeValue;
import org.sakaiproject.progress.model.data.entity.ProgressItem;
import org.sakaiproject.progress.model.data.entity.ProgressSiteConfiguration;
import org.sakaiproject.user.api.User;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *<p>
 *     Handles tracking Progress for the Assignment tool
 *</p>
 */

@Slf4j
public class ForumProgressImpl extends ProgressImpl implements IProgress {

    @Setter private DiscussionForumManager discussionForumManager;

    @Autowired ConfigurationManager configurationManager;

    /**
     * Gives a progress percentage mapped to users within a specific course.
     * @param siteID - Describes the portlet context - generated with ToolManager.getCurrentPlacement().getContext()
     * @param configWideAttributes
     * @return A Map of Progress Percentage that is mapped to user's UUID.
     */
    @Override
    public Map<String, Integer> getProgress(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {
    	List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();

        Map<String, Integer> result = new HashMap<>();

        ProgressRole role = getRole(siteID);

        //prevents errors if all progress items are null
        if(progressItems == null){
            if(role == ProgressRole.INSTRUCTOR || role == ProgressRole.TA) {
                for (User student : getStudents(siteID)) {
                    result.put(student.getId(), 0);
                }
            }
            else{
                for(User student : getStudents(siteID)){
                    if(student.getId().equalsIgnoreCase(getUser(siteID))){
                        result.put(student.getId(), 0);
                    }
                }
            }
        }
        else{
        	
        	int activeProgressItems = 0;

            //Loops through and calculates based off of active Progress Items
            for(ProgressItem progressItem: progressItems){

                if(progressItem.isActive()){
                	activeProgressItems++;
                    Map<String, Object> attributes = getProgressAttributeValue(progressItem);

                    List<DiscussionForum> forums = discussionForumManager.getDiscussionForumsByContextId(siteID);

                    int numTopics = 0;
                    int numForums = 0;

                    for(DiscussionForum df : forums){
                        if(df.getTitle().equalsIgnoreCase(progressItem.getName())){
                            List<Topic> topics = df.getTopics();

                            for(Topic topic : topics){
                                //Prevents a student from viewing the entire class
                                if(role == ProgressRole.INSTRUCTOR || role == ProgressRole.TA) {
                                    for (User student : getStudents(siteID)) {
                                        int progress = calculateUserProgress(student, topic, attributes);
                                        result = addToScore(result, student.getId(), progress);
                                    }
                                }
                                else{
                                    for (User student : getStudents(siteID)) {
                                        if(student.getId().equalsIgnoreCase(getUser(siteID))){
                                            int progress = calculateUserProgress(student, topic, attributes);
                                            result = addToScore(result, student.getId(), progress);
                                            break;
                                        }
                                    }
                                }
                                numTopics++;
                            }

                            if (numTopics > 0) {
	                            //Sets the value equally between each topic
	                            for(Map.Entry<String, Integer> entry : result.entrySet()){
	                                result.put(entry.getKey(), entry.getValue()/numTopics);
	                            }
                            }

                            numForums++;
                        }
                    }

                    if (numForums > 0) {
                        //Sets the value equally between each forum
                        for(Map.Entry<String, Integer> entry : result.entrySet()){
                            result.put(entry.getKey(), entry.getValue()/numForums);
                        }
                    }
                }
            }
            
            if(activeProgressItems > 0) {
	            for (Map.Entry<String, Integer> entry : result.entrySet()) {
	            	result.put(entry.getKey(), entry.getValue() / activeProgressItems);
	            }
            }
        }

        return result;
    }

    /**
     * Given a ProgressSiteConfiguration with an arbritary set of ProgressItems,
     * returns the same ProgressSiteConfiguration with the correct set of ProgressItems.
     * 
     * In practice this is used to get the original set of ProgressItems, and to update the set
     * whenever new ones need to be created (i.e. instructor adds a new assignment to the course.)
     * @param siteID
     * @return ProgressSiteConfiguration
     */
    @Override
    public ProgressSiteConfiguration getProgressItems(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {
        List<DiscussionForum> forums = discussionForumManager.getDiscussionForumsByContextId(siteID);

        ProgressBuilder progressBuilder = new ProgressBuilder();

        //Adds any new forums to the Configurations
        for(DiscussionForum forum : forums){
        	//if the forum has no topics then it is not 'active' and should not be considered
        	if (forum.getTopicsSet().size() > 0) {
	            boolean found = false;
	
	            for(ProgressItem pi : progressSiteConfiguration.getProgressItems()){
	                if(pi.getName().equals(forum.getTitle())){
	                    found = true;
	                }
	            }
	
	            if(!found){
	                ProgressItem temp = progressBuilder.buildProgressItem(forum.getTitle(), progressSiteConfiguration);
	                progressSiteConfiguration.getProgressItems().add(temp);
	            }
        	}
        }
        
        List<ProgressItem> itemsToRemove = new ArrayList<ProgressItem>();

        //Adds progress items to delete because the forum does not exist
        if(progressSiteConfiguration.getProgressItems() != null){
            for(ProgressItem pi : progressSiteConfiguration.getProgressItems()){
                boolean found = false;

                for(DiscussionForum forum : forums){
                	//if the forum has no topics then it is not 'active' and should not be considered
                	if (forum.getTopicsSet().size() > 0) {
	                    if(pi.getName().equals(forum.getTitle())){
	                        found = true;
	                    }
                	}
                }

                if(!found){
                	itemsToRemove.add(pi);
                }
            }
        }

        //Removes the unneeded progress items
        if (itemsToRemove.size() > 0) {
        	progressSiteConfiguration.getProgressItems().removeAll(itemsToRemove);
        }

        return progressSiteConfiguration;
    }

    /**
     * returns the completion percentage for each progress item for a student. This method should check to make sure the user requesting
     * the information is an instructor or a ta. If neither the current user should have the same uid as the uid param.
     * This is meant for situations where an item can be partially complete.
     * In many cases this will just be 100% (An item is either complete or it is not.)
     * @param siteID
     * @param uid
     * @param progressSiteConfiguration
     * @return Map
     */
    @Override
    public Map<ProgressItem, Integer> getCompletionPercentagesForStudent(String siteID, String uid, ProgressSiteConfiguration progressSiteConfiguration) {

        ProgressRole role = this.getRole(siteID);

        Map<ProgressItem, Integer> percentageMap = new HashMap<>();

        //Prevents students from viewing information for other students
        if (role != ProgressRole.INSTRUCTOR && role != ProgressRole.TA) {
            if (!(uid.equals(getUser(siteID)))) {
                throw new ProgressServiceException("Current user does not have access to this student");
            }
        }

        List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();

        //Loops through all ProgressItems and calculates based off of active items
        for(ProgressItem progressItem: progressItems){

            if(progressItem.isActive()){

                Map<String, Object> attributes = getProgressAttributeValue(progressItem);

                List<DiscussionForum> forums = discussionForumManager.getDiscussionForumsByContextId(siteID);
                
                int numTopics = 0;

                for(DiscussionForum df : forums){
                    if(df.getTitle().equalsIgnoreCase(progressItem.getName())){
                        List<Topic> topics = df.getTopics();
                        for(Topic topic : topics){
                            for (User student : getStudents(siteID)) {
                                if(student.getId().equalsIgnoreCase(uid)){
                                        int progress = calculateUserProgress(student, topic, attributes);
                                        if (percentageMap.containsKey(progressItem)) {
                                        	percentageMap.put(progressItem, percentageMap.get(progressItem) + progress);
                                        }
                                        else {
                                        	percentageMap.put(progressItem, progress);
                                        }
                                        numTopics++;
                                        break;
                                }
                            }
                        }
                        
                        if (numTopics > 0) {
                        	percentageMap.put(progressItem, percentageMap.get(progressItem) / numTopics);
                        }
                    }
                }
            }


        }
        return percentageMap;
    }

    /**
     * gets the total percentage of the Progress Item in the implementation. Ex. There are 10 Progress items and each progress item is equally weighted,
     * each progress item should be worth 10 percent.
     *
     * @param siteID
     * @param progressSiteConfiguration
     * @return Map
     */
    @Override
    public Map<ProgressItem, Integer> getProgressItemPercentageOfImplementation(String siteID, ProgressSiteConfiguration progressSiteConfiguration) {
        Map<ProgressItem, Integer> weights = new HashMap<>();

        List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();
        int numItems = 0;
        for(ProgressItem progressItem : progressItems){
            if(progressItem.isActive()){
                numItems++;
            }
        }
        for(ProgressItem progressItem : progressItems){
            if(progressItem.isActive()){
                weights.put(progressItem, (100/numItems));
            }
        }
        return weights;
    }

    /**
     * This method returns the total completion percentage of the implementation for the specified user.
     * @param progressSiteConfiguration
     * @param siteID
     * @param uid
     * @return Double
     */
    @Override
    public Double getImplementationCompletionPercentage(ProgressSiteConfiguration progressSiteConfiguration, String siteID, String uid) {

        List<ProgressItem> progressItems = progressSiteConfiguration.getProgressItems();

        Map<String, Integer> result = new HashMap<>();
        
        int activeProgressItems = 0;

        //Loops through each ProgressItem and sums up the completion percentages of each active one
        for(ProgressItem progressItem: progressItems){

            if(progressItem.isActive()){
            	activeProgressItems++;
                Map<String, Object> attributes = getProgressAttributeValue(progressItem);

                List<DiscussionForum> forums = discussionForumManager.getDiscussionForumsByContextId(siteID);

                int numTopics = 0;
                int numForums = 0;

                //Matches the DiscussionForum to the ProgressItem
                for(DiscussionForum df : forums){
                    if(df.getTitle().equalsIgnoreCase(progressItem.getName())){
                        List<Topic> topics = df.getTopics();


                        //Calculate progress for each topic
                        for(Topic topic : topics){
                            for (User student : getStudents(siteID)) {
                                if(student.getId().equalsIgnoreCase(uid)){
                                    int progress = calculateUserProgress(student, topic, attributes);
                                    result = addToScore(result, student.getId(), progress);
                                    break;
                                }
                            }
                            numTopics++;
                        }

                        if (numTopics > 0) {
                            //Divides the number of points evenly between each topic
                            for(Map.Entry<String, Integer> entry : result.entrySet()){
                                result.put(entry.getKey(), entry.getValue()/numTopics);
                            }                        	
                        }

                        numForums++;
                    }
                }
                
                if (numForums > 0) {
	                //Divides the number of points evenly between each forum
	                for(Map.Entry<String, Integer> entry : result.entrySet()){
	                    result.put(entry.getKey(), entry.getValue()/numForums);
	                }
                }
            }
        }
        
        if(activeProgressItems > 0) {
            for (Map.Entry<String, Integer> entry : result.entrySet()) {
            	result.put(entry.getKey(), entry.getValue() / activeProgressItems);
            }
        }

        //There are no progress items defined for Forum
        if (result.size() < 1) {
        	return 100.0;
        }
        else {
        	return result.get(uid).doubleValue();
        }
    }

    /**
     * Returns the Name of the implementation (used as the identifier in the database.)
     * @return the name of the implementation.
     */
    @Override
    public String getName() {
        return "Forum";
    }

    /**
     * Gets the list of Attributes that this implementation uses for configurations.
     * @return The list of Attributes.
     */
    @Override
    public List<Attribute> getAttributes() {
        Attribute postWordCount = new Attribute();
        postWordCount.setDefaultValue("0");
        postWordCount.setAttributeType("double");
        postWordCount.setConfigWideAttribute(false);
        postWordCount.setName("Post Word Count");

        Attribute replyWordCount = new Attribute();
        replyWordCount.setDefaultValue("0");
        replyWordCount.setAttributeType("double");
        replyWordCount.setConfigWideAttribute(false);
        replyWordCount.setName("Reply Word Count");

        Attribute numOfReplies = new Attribute();
        numOfReplies.setDefaultValue("0");
        numOfReplies.setAttributeType("double");
        numOfReplies.setConfigWideAttribute(false);
        numOfReplies.setName("Number of Replies");

        Attribute postExists = new Attribute();
        postExists.setDefaultValue("false");
        postExists.setAttributeType("boolean");
        postExists.setConfigWideAttribute(false);
        postExists.setName("Post Exists");

        Attribute replyExists = new Attribute();
        replyExists.setDefaultValue("false");
        replyExists.setAttributeType("boolean");
        replyExists.setConfigWideAttribute(false);
        replyExists.setName("Reply Exists");

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(postWordCount);
        attributes.add(replyWordCount);
        attributes.add(numOfReplies);
        attributes.add(postExists);
        attributes.add(replyExists);

        return attributes;
    }

    /**
     * Returns the validation result depending on whether the input siteConfig is valid.
     * This should check that each individual attribute has a valid value
     * and also should make sure that all the attribute values make sense together
     * (i.e. all weights add up to 100).
     * @param potentialSiteConfig
     * @return
     */
    @Override
	public ValidationResult isValid(ProgressSiteConfiguration potentialSiteConfig) {
		ValidationResult result = new ValidationResult();

		List<ProgressItem> progressItems = potentialSiteConfig.getProgressItems();

		//Loops through each ProgressItem
		for(ProgressItem progressItem : progressItems){

		    //Checks the post config values for appropriate values
		    if(Boolean.parseBoolean(progressItem.getAttributeValueByName("Post Exists"))){
		        Double postWords = 0.0;
		        try{
		            postWords = Double.parseDouble(progressItem.getAttributeValueByName("Post Word Count"));
		            if(postWords < 0){
		                result.setValid(false);
                        result.setErrorMessage(result.getErrorMessage() + "\nNumber of words on a post must be 0 or more.");
		                return result;
                    }
                }
		        catch (NumberFormatException e){
		            result.setValid(false);
                    result.setErrorMessage(result.getErrorMessage() + "\nNumber of words on posts require numeric input only.");
		            return result;
                }
            }

		    //Checks the reply config values for appropriate values
		    if(Boolean.parseBoolean(progressItem.getAttributeValueByName("Reply Exists"))) {
                Double numberOfReplies = 0.0;
                Double replyWords = 0.0;

                try {
                    numberOfReplies = Double.parseDouble(progressItem.getAttributeValueByName("Number of Replies"));

                    if(numberOfReplies < 0){
                        result.setValid(false);
                        result.setErrorMessage(result.getErrorMessage() + "\nNumber of replies must be 0 or more.");
                        return result;
                    }
                }catch (NumberFormatException e){
                    result.setValid(false);
                    result.setErrorMessage(result.getErrorMessage() + "\nNumber of Replies require numeric input only.");
                    return result;
                }

                //Checks num of replies config values
                if(numberOfReplies > 0){
                    try{
                        replyWords = Double.parseDouble(progressItem.getAttributeValueByName("Reply Word Count"));

                        if(replyWords < 0){
                            result.setValid(false);
                            result.setErrorMessage(result.getErrorMessage() + "\nNumber of words on a reply must be 0 or more.");
                            return result;
                        }
                    }catch (NumberFormatException e){
                        result.setValid(false);
                        result.setErrorMessage(result.getErrorMessage() + "\nReply Word Counts require numeric input only.");
                        return result;
                    }
                }
            }
        }
		result.setValid(true);
		return result;
	}

    /**
     * Gets the values of the attributes for the forum implementation for a site
     * @param progressItem
     * @return Map
     */
    private Map<String, Object> getProgressAttributeValue(ProgressItem progressItem){
        Map<String, Object> result = new HashMap<>();

        for(ProgressAttributeValue progressAttributeValue : progressItem.getValues()){
            if(progressAttributeValue.getAttributeName().equalsIgnoreCase("Post Word Count")){
                result.put("Post Word Count", Integer.parseInt(progressAttributeValue.getValue()));
            }
            else if(progressAttributeValue.getAttributeName().equalsIgnoreCase("Reply Word Count")){
                result.put("Reply Word Count", Integer.parseInt(progressAttributeValue.getValue()));
            }
            else if(progressAttributeValue.getAttributeName().equalsIgnoreCase("Number of Replies")){
                result.put("Number of Replies", Integer.parseInt(progressAttributeValue.getValue()));
            }
            else if(progressAttributeValue.getAttributeName().equalsIgnoreCase("Post Exists")){
                result.put("Post Exists", Boolean.parseBoolean(progressAttributeValue.getValue()));
            }
            else if(progressAttributeValue.getAttributeName().equalsIgnoreCase("Reply Exists")){
                result.put("Reply Exists", Boolean.parseBoolean(progressAttributeValue.getValue()));
            }
        }

        return result;
    }

    /**
     * Parses out the html from a post or reply.
     * @param body
     * @return String
     */
    private String parseBodyHtml(String body){
        Reader temp= new StringReader(body);

        StringBuffer s = new StringBuffer();

        ParserDelegator delegator = new ParserDelegator();
        ParserCallback parserCallback = new ParserCallback(){
            @Override
            public void handleText(char[] data, int pos) {
                s.append(data);
            }
        };

        try {
            delegator.parse(temp, parserCallback, Boolean.TRUE);

            return s.toString();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Calculates a User's progress for a particular topic
     * @param student
     * @param topic
     * @param attributes
     * @return Integer
     */
    private Integer calculateUserProgress(User student, Topic topic, Map<String, Object> attributes){
        int postWordCount = (Integer)attributes.get("Post Word Count");
        int replyWordCount = (Integer)attributes.get("Reply Word Count");
        int numberOfReplies = (Integer)attributes.get("Number of Replies");
        boolean postExists = (Boolean)attributes.get("Post Exists");
        boolean replyExists = (Boolean)attributes.get("Reply Exists");

        List<Message> posts = new ArrayList<>();
        List<Message> replies = new ArrayList<>();
        for (Message m : topic.getMessages()) {
            if (m.getAuthorId().equalsIgnoreCase(student.getId())) {
                if (m.getInReplyTo() == null) {
                    posts.add(m);
                } else {
                    replies.add(m);
                }
            }
        }

        int totalScore = 0;
        int totalItems = 1;

        if (replyExists && numberOfReplies <= 0) {
            totalItems++;
        } else if (replyExists && numberOfReplies > 0) {
            totalItems += numberOfReplies;
        }

        if (postExists && postWordCount <= 0) {
            if (posts.size() > 0) {
                totalScore += 100;
            }
        } else if (postExists && postWordCount > 0) {
            if (posts.size() > 0) {
                for (Message post : posts) {
                    String body = parseBodyHtml(post.getBody());
                    if (body.split(" ").length >= postWordCount) {
                        totalScore += 100;
                        break;
                    }
                }
            }
        }
        else{
            totalScore += 100;
        }

        if (replyExists && numberOfReplies <= 0) {
            if (replies.size() > 0) {
                totalScore += 100;
            }
        } else if (replyExists && numberOfReplies > 0 && replyWordCount <= 0) {
            if (replies.size() >= numberOfReplies) {
                totalScore += (100 * numberOfReplies);
            }
            else if (replies.size() > 0) {
            	totalScore += (100 * replies.size());
            }
        } else if (replyExists && numberOfReplies > 0 && replyWordCount > 0) {
            if (replies.size() >= numberOfReplies) {

                for (Message reply : replies) {
                    String body = parseBodyHtml(reply.getBody());

                    if ((body.split(" ").length >= replyWordCount)) {
                        totalScore += 100;
                        numberOfReplies--;

                        if (numberOfReplies == 0) {
                            break;
                        }
                    }
                }
            }
        }

        return totalScore/totalItems;
    }

    private Map<String, Integer> addToScore(Map<String, Integer> result, String studentId, int progress){
        if(result.containsKey(studentId)){
            result.put(studentId, result.get(studentId) + progress);
        }
        else{
            result.put(studentId, progress);
        }

        return result;
    }
}
