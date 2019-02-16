package com.hubmor.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import org.primefaces.model.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.hubmor.DTO.SignUpAttemptDTO;
import com.hubmor.DTO.UserBasicInfoDTO;
import com.hubmor.DTO.UserTimelineDTO;
import com.hubmor.DTO.UsersDTO;
import com.hubmor.model.UserBasicInfo;
import com.hubmor.model.UserTimeline;
import com.hubmor.model.Users;
import com.hubmor.service.SignUpAttemptService;
import com.hubmor.service.TimelineService;
import com.hubmor.service.UserBasicInfoService;
import com.hubmor.service.UserService;
import com.hubmor.signup.EmailSending;
import com.hubmor.signup.EmailSendingHelper;
import com.hubmor.upload.Upload;
import com.hubmor.upload.UploadImpl;
import com.hubmor.upload.UploadImplStatus;
import com.hubmor.upload.UploadStatus;



@Controller
@ManagedBean
@Scope("session")
public class LoginController implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@Autowired
	private UserService userService;
	@Autowired
	private SignUpAttemptService signUpAttemptService;
	@Autowired
	private EmailSending emailSending;
	@Autowired
	private EmailSendingHelper emailSendingHelper;
	@Autowired
	private UserBasicInfoService userBasicInfoService;
	@Autowired
	private TimelineService timelineService;
	
	private UserBasicInfoDTO userBasicInfoDTO;
	private SignUpAttemptDTO signUpAttemptDTO;
	private UsersDTO usersDTO;
	private UserTimelineDTO userTimelineDTO;
	private String activationCode;
	private String password;
	private String status;
	

	private UploadedFile uploadFile;
	private UploadedFile uploadedFile;
	
	private List<UsersDTO> usersDTOList;
	private List<UserBasicInfoDTO> userBasicInfoList;
	private List<UserTimelineDTO> userTimelineList;
	private List<UserTimelineDTO> userTimeList;
	
	private String loginErrorMsg;
	private boolean userInValidity;

	
	
	/*public String checkUserValidity(){
		boolean userExistence=false;
		userExistence=userService.isUserExist(userBasicInfoDTO.getEmail());		
		if(userExistence){
			return "login-password.xhtml?faces-redirect=true";
		}else{
			return "timeline.xhtml?faces-redirect=true";
		}		
	}*/

	public String signUp(){
		FacesContext context=FacesContext.getCurrentInstance();		
		activationCode="";
		
		long generatedCodeHelper1=999999;
		long autogeneratedCodeHelper2 = generatedCodeHelper1 + new Date().getTime();		
		long finalAutogeneratedCode = autogeneratedCodeHelper2 % 10000;
		try{
			signUpAttemptService.deleteExtraObject(userBasicInfoDTO.getEmail()); /*delete Extra object by given user email */
			
			signUpAttemptDTO=new SignUpAttemptDTO();
			signUpAttemptDTO.setActivationCode(String.valueOf(finalAutogeneratedCode));
			signUpAttemptDTO.setDate(new Date());
			signUpAttemptDTO.setStatus(1);
			signUpAttemptDTO.setEmail(userBasicInfoDTO.getEmail());  
			signUpAttemptService.createSignUpAttempt(signUpAttemptDTO);/* save activation code in signUpAttempt table*/

			emailSending.setReceipient(userBasicInfoDTO.getEmail());
			emailSending.setSubject("Simple Application Activation Code");
			emailSending.setContent("Your Accout Activation Code for is: " + finalAutogeneratedCode); 
			emailSendingHelper.setEmailSending(emailSending); 
			emailSendingHelper.sendMail();			/*will be send of code in user email,by given user email*/
		}catch(Exception e){
			context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,"Failed!",""));
			e.printStackTrace();
		}		
		context.addMessage(null, new FacesMessage("An Activation Code sent to your email"));
		
		System.out.println("Activation Code: "+activationCode);

		
		return "activation.xhtml?faces-redirect=true";
	}
	
	public String activateUser(){
		FacesContext context=FacesContext.getCurrentInstance();
		try{
			if(activationCode.equals(signUpAttemptDTO.getActivationCode())){		
			userBasicInfoService.saveUserBasicInfo(userBasicInfoDTO); /* save user basic infromation  in userBasicInfo table*/
			UsersDTO usersDTO=new UsersDTO();
			usersDTO.setUserName(userBasicInfoDTO.getEmail());
			usersDTO.setEnabled(true);
			usersDTO.setPassword(userBasicInfoDTO.getPassword());
			UserBasicInfo userBasicInfo=new UserBasicInfo();
			userBasicInfo=userBasicInfoService.findByEmail(userBasicInfoDTO.getEmail());  /*find user userBasicInfo ID from UserBasicInfo table*/
			usersDTO.setUserBasicInfo(userBasicInfo);
			userService.saveUser(usersDTO);	 /* save user id and password  in user table*/
			password=null;
		}
			context.addMessage(null, new FacesMessage("Your account has been activated successfully!"));
		}catch(Exception e){
			context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,"Your account could not be activated!",""));
			e.printStackTrace();
		}
			return "login.xhtml?faces-redirect=true";
	}
	
	public String login(){
		Users user=new Users();		
		user=userService.findByUserName(usersDTO.getUserName());  /*find user account from user table, for user login*/
		System.out.println("request accepted..."+usersDTO.getUserName()+getPassword());
		if(user.getPassword().equals(password)){
			userInValidity=false;
			return "user/timeline.xhtml?faces-redirect=true";
		}else{
			loginErrorMsg="Sorry! You have No Account in hubmor";
			return "signUp.xhtml?faces-redirect=true";
		}	
	}
	
	 /*that is used for show user all information by user email from userBasicInformation table.it profile page is shown*/
	public void findUserWiseList(){
		userBasicInfoList=new ArrayList<>();
		userBasicInfoList=userService.findByUser(usersDTO.getUserName());
	}
	
	/*that is used for show all individual user status from UserTimeline table, find by user email. it profile page is shown*/
	public void findUserWiseStatus(){
		userTimelineList=new ArrayList<>();
		userTimelineList=timelineService.findByStatus(usersDTO.getUserName());
		Collections.reverse(userTimelineList);
	}
	
	/*that is used for save user status in UserTimeline table. it save from timeline page*/
	public void statusSave(){
		UserBasicInfo userBasicInfo=new UserBasicInfo();
		userBasicInfo=userBasicInfoService.findById(usersDTO.getUserName()); /*find UserID/personalID from UserBasicInfo table*/
		userTimelineDTO.setUserBasicInfo(userBasicInfo);
		userTimelineDTO.setDate(new Date());
		
		/*UploadStatus upload=new UploadImplStatus();
		String fileName=uploadedFile.getFileName();
		upload.upload("status", fileName, uploadedFile);
		
		if(status!=null){
			userTimelineDTO.setStatus(status);
			timelineService.statusSave(userTimelineDTO);
		}else if(fileName!=null){
			userTimelineDTO.setStatus(fileName);
			timelineService.statusSave(userTimelineDTO);
		}
		*/
		
		timelineService.statusSave(userTimelineDTO);
		userTimelineDTO=null;
	}
	
	/*that is used for show all status or experts wise user status from UserTimeline table, it timeline page is shown*/
	public void findAllTimeline(){
		userTimeList =new ArrayList<>();
		UserBasicInfo users=new UserBasicInfo();
		users=timelineService.findByUserExperts(usersDTO.getUserName()); /*find User Experts from UserBasicInfo table*/
		String expert=users.getExperts();
		if(expert!=null){
			userTimeList=timelineService.findExpertsByStatus(users.getExperts()); /*find User Experts wise status from UserTimeline table*/
			Collections.reverse(userTimeList);
		}else {
			userTimeList=timelineService.findAllTimeline(); /*find All user status from UserTimeline table*/
			Collections.reverse(userTimeList);
		}
		
		/*userTimeList=timelineService.findAllTimeline();
		Collections.reverse(userTimeList);*/
	}
	
	/*that is used for edit user information in userBasicInformation table, it profile page is shown*/
	public void editUser(){
		FacesContext mass=FacesContext.getCurrentInstance();
		try{	
			Upload upload=new UploadImpl();
			String fileName=uploadFile.getFileName();
			upload.upload("picture", fileName, uploadFile);
			userBasicInfoDTO.setPhotoPath(fileName);
			userService.editUser(userBasicInfoDTO);
			mass.addMessage(null, new FacesMessage("User Update Successfully!"));
		}catch (Exception e) {
			mass.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,"User Could not Update Successfully!",""));
 			
		}
	}
	
	/*that is used for logout, it header page is shown*/
	public void logout(){
		userInValidity=true;
	}
	
	public UserBasicInfoDTO getUserBasicInfoDTO() {
		if(userBasicInfoDTO==null) userBasicInfoDTO=new UserBasicInfoDTO();
		return userBasicInfoDTO;
	}

	public void setUserBasicInfoDTO(UserBasicInfoDTO userBasicInfoDTO) {
		this.userBasicInfoDTO = userBasicInfoDTO;
	}

	public SignUpAttemptDTO getSignUpAttemptDTO() {
		if(signUpAttemptDTO==null) signUpAttemptDTO=new SignUpAttemptDTO();
		return signUpAttemptDTO;
	}

	public void setSignUpAttemptDTO(SignUpAttemptDTO signUpAttemptDTO) {
		this.signUpAttemptDTO = signUpAttemptDTO;
	}

	public String getActivationCode() {
		return activationCode;
	}

	public void setActivationCode(String activationCode) {
		this.activationCode = activationCode;
	}

	public UsersDTO getUsersDTO() {
		if(usersDTO==null) usersDTO=new UsersDTO();
		return usersDTO;
	}

	public void setUsersDTO(UsersDTO usersDTO) {
		this.usersDTO = usersDTO;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLoginErrorMsg() {
		return loginErrorMsg;
	}

	public void setLoginErrorMsg(String loginErrorMsg) {
		this.loginErrorMsg = loginErrorMsg;
	}

	public boolean isUserInValidity() {
		return userInValidity;
	}

	public void setUserInValidity(boolean userInValidity) {
		this.userInValidity = userInValidity;
	}

	public List<UsersDTO> getUsersDTOList() {
		
		return usersDTOList;
	}

	public void setUsersDTOList(List<UsersDTO> usersDTOList) {
		this.usersDTOList = usersDTOList;
	}

	public List<UserBasicInfoDTO> getUserBasicInfoList() {
		findUserWiseList();
		return userBasicInfoList;
	}

	public void setUserBasicInfoList(List<UserBasicInfoDTO> userBasicInfoList) {
		this.userBasicInfoList = userBasicInfoList;
	}

	public UserTimelineDTO getUserTimelineDTO() {
		if(userTimelineDTO ==null) userTimelineDTO=new UserTimelineDTO();
		return userTimelineDTO;
	}

	public void setUserTimelineDTO(UserTimelineDTO userTimelineDTO) {
		this.userTimelineDTO = userTimelineDTO;
	}

	public List<UserTimelineDTO> getUserTimelineList() {
		findUserWiseStatus();
		return userTimelineList;
	}

	public void setUserTimelineList(List<UserTimelineDTO> userTimelineList) {
		this.userTimelineList = userTimelineList;
	}

	public UploadedFile getUploadFile() {
		return uploadFile;
	}

	public void setUploadFile(UploadedFile uploadFile) {
		this.uploadFile = uploadFile;
	}

	public List<UserTimelineDTO> getUserTimeList() {
		findAllTimeline();
		return userTimeList;
	}

	public void setUserTimeList(List<UserTimelineDTO> userTimeList) {
		this.userTimeList = userTimeList;
	}

	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
}