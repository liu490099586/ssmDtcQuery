package com.xtool.query.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.xtool.query.po.CarCustom;
import com.xtool.query.po.CarDTO;
import com.xtool.query.po.DtcCustom;
import com.xtool.query.po.DtcDTO;
import com.xtool.query.po.DtcQueryVo;
import com.xtool.query.po.MessageDTO;
import com.xtool.query.po.UserCustom;
import com.xtool.query.po.UserDTO;
import com.xtool.query.po.UserQueryVo;
import com.xtool.query.service.CarService;
import com.xtool.query.service.DtcService;
import com.xtool.query.service.UserService;
import com.xtool.query.utils.CodingUtils;
import com.xtool.query.utils.RSAUtils;
import com.xtool.query.utils.RandomUtils;

@Controller
@RequestMapping("/app")
public class AppController {

	@Autowired
	private DtcService dtcService;
	@Autowired
	private UserService userService;
	@Autowired
	private CarService carService;
	private String privateKeyPath = "D:\\file\\privateKey.cer"; 
	private String publicKeyPath = "D:\\file\\publicKey.cer"; 
	
	@RequestMapping("/queryDtcByDcodeJson")
	public @ResponseBody MessageDTO<List<DtcDTO>> queryDtcByDcodeJson(@RequestBody DtcCustom dtcCustom) throws Exception {
		MessageDTO<List<DtcDTO>> message = new MessageDTO<List<DtcDTO>>();
		String dcode = null;
		message.setCode(0);
		message.setMsg("");
		try {
			CodingUtils.deCodingData(dtcCustom.getKey(), dtcCustom);
			dcode = dtcCustom.getDcode();
			dtcCustom.setDcode(dcode);
			DtcQueryVo dtcQueryVo = new DtcQueryVo();
			dtcQueryVo.setCustom(dtcCustom);
			dtcQueryVo.setS(dtcCustom.getS());
			dtcQueryVo.setPs(dtcCustom.getPs());
			List<DtcDTO> dtcList = dtcService.findDtcListByQuery(dtcQueryVo);
			String uuid = RandomUtils.getRandomValue(16);
			for(DtcDTO dtc : dtcList) {
				dtc.setKey(uuid);
				CodingUtils.encodingByPrivateKey(dtc,uuid);
			}
			message.setData(dtcList);
		}catch (Exception e) {
			message.setCode(101);
			message.setMsg("密钥错误");
		}
		return message;
	}
	
	@RequestMapping("/userLogin")
	public @ResponseBody MessageDTO<List<UserDTO>> userLogin(@RequestBody UserCustom userCustom) {
		MessageDTO<List<UserDTO>> message = new  MessageDTO<List<UserDTO>>();
		message.setCode(0);
		message.setMsg("");
		try {
			CodingUtils.deCodingData(userCustom.getKey(), userCustom);
			UserQueryVo userQueryVo = new UserQueryVo();
			userQueryVo.setCustom(userCustom);
			List<UserDTO> userList = userService.findUserDTOListByUnamePage(userQueryVo);
			if(userList.size() != 1) {
				message.setCode(201);
				message.setMsg("用户名或密码错误");
			}else {
				//设置登录状态
				userService.updateLoginByUname(userCustom);
				String uuid = RandomUtils.getRandomValue(16);
				for(UserDTO user : userList) {
					CarDTO carDTO = user.getCarDTO();
					if(carDTO != null) {
						carDTO.setKey(uuid);
						CodingUtils.encodingByPrivateKey(carDTO,uuid);
						user.setCarDTO(carDTO);
					}
					user.setKey(uuid);
					CodingUtils.encodingByPrivateKey(user,uuid);
				}
				message.setCode(0);
				message.setMsg("");
				message.setData(userList);
			}
			
		}catch (Exception e) {
			message.setCode(101);
			message.setMsg("密钥错误");
		}
		return message;
	}
	
	@RequestMapping("/userLogout")
	public @ResponseBody MessageDTO<List<UserDTO>> userLogout(@RequestBody UserCustom userCustom) {
		MessageDTO<List<UserDTO>> message = new MessageDTO<List<UserDTO>> ();
		message.setCode(0);
		message.setMsg("");
		List<UserDTO> list = new ArrayList<UserDTO>();
		try {
			CodingUtils.deCodingData(userCustom.getKey(), userCustom);
			userService.updateLoginByUname(userCustom);
			String uuid = RandomUtils.getRandomValue(16);
			UserDTO userDTO = new UserDTO();
			BeanUtils.copyProperties(userCustom, userDTO);
			userDTO.setKey(uuid);
			userDTO.setIslogin("logout");
			CodingUtils.encodingByPrivateKey(userDTO,uuid);
			list.add(userDTO);
			message.setData(list);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return message;
	}
	
	
	
	@RequestMapping("/upasswordUpdate")
	public @ResponseBody MessageDTO<List<UserDTO>> upasswordUpdate(@RequestBody UserCustom userCustom) {
		MessageDTO<List<UserDTO>> message = new  MessageDTO<List<UserDTO>>();
		
		message.setCode(0);
		message.setMsg("");
		UserDTO userDTO;
		try {
			CodingUtils.deCodingData(userCustom.getKey(), userCustom);
			UserCustom user = userService.findUserByUname(userCustom.getUname());
			userService.updateUser(user.getUid(), userCustom);
			UserCustom custom = userService.findUserById(user.getUid());
			userDTO = new UserDTO();
			BeanUtils.copyProperties(custom, userDTO);
			String uuid = RandomUtils.getRandomValue(16);
			userDTO.setKey(uuid);
			CodingUtils.encodingByPrivateKey(userDTO, uuid);
			List<UserDTO> list = new ArrayList<UserDTO>();
			list.add(userDTO);
			message.setData(list);
		}catch (Exception e) {
			message.setCode(101);
			message.setMsg("密钥错误");
		}
		
		return message;
	}
	
	
	@RequestMapping("/userinfoUpdate")
	public @ResponseBody MessageDTO<List<UserDTO>> userinfoUpdate(@RequestBody UserCustom userCustom) {
		MessageDTO<List<UserDTO>> message = new MessageDTO<List<UserDTO>>();
		message.setCode(0);
		message.setMsg("");
		String sAesKey = null;
		try {
			sAesKey = RSAUtils.decryptByPrivateKey(privateKeyPath, userCustom.getKey());
			CodingUtils.DeCoding(userCustom, sAesKey);
			UserQueryVo userquery = new UserQueryVo();
			userquery.setCustom(userCustom);
			List<UserDTO> userinfoList = userService.findUserDTOListByUname(userquery);
			if(userinfoList.get(0).getCarDTO() != null 
					&& userinfoList.get(0).getCarDTO().getCuid() != null
					&& !userinfoList.get(0).getCarDTO().getCuid().equals("")) {
				userService.updateCarByUname(userCustom);
			}else {
				//插入car表数据
				CarCustom car = new CarCustom();
				BeanUtils.copyProperties(userCustom.getCarDTO(), car);
				UserCustom findUserByUname = userService.findUserByUname(userCustom.getUname());
				car.setCuid(findUserByUname.getUid());
				carService.insertCar(car);
			}
			UserQueryVo userQueryVo = new UserQueryVo();
			userQueryVo.setCustom(userCustom);
			List<UserDTO> userList = userService.findUserDTOListByUname(userQueryVo);
			if(userList.size() != 1) {
				message.setCode(201);
				message.setMsg("用户名或密码错误");
			}else {
				String uuid = RandomUtils.getRandomValue(16);
				for(UserDTO user : userList) {
					CarDTO carDTO = user.getCarDTO();
					if(carDTO != null) {
						carDTO.setKey(uuid);
						CodingUtils.encodingByPrivateKey(carDTO,uuid);
						user.setCarDTO(carDTO);
					}
					user.setKey(uuid);
					CodingUtils.encodingByPrivateKey(user,uuid);
				}
				message.setCode(0);
				message.setMsg("");
				message.setData(userList);
			}
		} catch (Exception e) {
			message.setCode(101);
			message.setMsg("密钥错误");
		}
		
		return message;
	}
	
	@RequestMapping("/userInsert")
	public @ResponseBody MessageDTO<List<UserDTO>> userInsert(@RequestBody UserCustom userCustom) {
		MessageDTO<List<UserDTO>> message = new MessageDTO<List<UserDTO>>();
		message.setCode(0);
		message.setMsg("");
		List<UserDTO> userList = new ArrayList<UserDTO>();
		String sAesKey = null;
		try {
			sAesKey = RSAUtils.decryptByPrivateKey(privateKeyPath, userCustom.getKey());
			CodingUtils.DeCoding(userCustom, sAesKey);
			UserCustom custom = userService.findUserByUname(userCustom.getUname());
			if(custom != null) {
				message.setCode(202);
				message.setMsg("用户名重复");
			}else {
				userService.insertUser(userCustom);
				message.setCode(0);
				message.setMsg("");
				message.setData(userList);
			}
		} catch (Exception e) {
			message.setCode(101);
			message.setMsg("密钥错误");
		}
		
		return message;
	}
}
