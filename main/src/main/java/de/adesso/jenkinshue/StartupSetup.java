package de.adesso.jenkinshue;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.adesso.jenkinshue.common.dto.bridge.BridgeDTO;
import de.adesso.jenkinshue.common.dto.team.TeamCreateDTO;
import de.adesso.jenkinshue.common.dto.team.TeamLampsDTO;
import de.adesso.jenkinshue.common.dto.user.UserCreateDTO;
import de.adesso.jenkinshue.common.dto.user.UserDTO;
import de.adesso.jenkinshue.common.dto.user.UserUpdateDTO;
import de.adesso.jenkinshue.common.enumeration.Role;
import de.adesso.jenkinshue.common.service.BridgeService;
import de.adesso.jenkinshue.common.service.HueService;
import de.adesso.jenkinshue.common.service.TeamService;
import de.adesso.jenkinshue.common.service.UserService;
import de.adesso.jenkinshue.entity.Bridge;
import de.adesso.jenkinshue.entity.User;
import de.adesso.jenkinshue.exception.InvalidLoginException;
import de.adesso.jenkinshue.exception.UserAlreadyExistsException;
import de.adesso.jenkinshue.repository.BridgeRepository;
import de.adesso.jenkinshue.repository.TeamRepository;
import de.adesso.jenkinshue.repository.UserRepository;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class StartupSetup {

	@Autowired
	private TeamRepository teamRepository;
	
	@Autowired
	private TeamService teamService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private BridgeService bridgeService;
	
	@Autowired
	private HueService hueService;
	
	@Value("#{'${admins}'.split(',')}")
	private List<String> admins;
	
	@PostConstruct
	public void init() {
		
		/*
		 * create admins
		 */
		if(teamRepository.findAll().isEmpty()) {
			TeamLampsDTO team = teamService.create(new TeamCreateDTO("Team 1"));
			
			UserDTO anAdmin = null;
			
			for(String admin : admins) {
				admin = admin.trim();
				try {
					UserDTO tmp = userService.create(new UserCreateDTO(admin, team.getId()));
					if(anAdmin == null) {
						anAdmin = tmp;
					}
				} catch(UserAlreadyExistsException uaee) {
					log.error(uaee);
				} catch(InvalidLoginException ile) {
					log.error(ile);
				}
			}

			// create bridge with ip, hueUserName and user if necessary
		}
		
		/*
		 * set roles (every start)
		 */
		for(String admin : admins) {
			UserDTO user = userService.findByLogin(admin.trim());
			if(user != null) {
				List<Role> roles = user.getRoles();
				if(roles == null) {
					roles = new ArrayList<>();
					roles.add(Role.ROLE_ADMIN);
					
				} else if (!roles.contains(Role.ROLE_ADMIN)) {
					roles.add(Role.ROLE_ADMIN);
				}
				userService.update(new UserUpdateDTO(user.getId(), roles));
			}
		}
		
		List<BridgeDTO> bridgesInDB = bridgeService.findAll();
		for (BridgeDTO bridge : bridgesInDB) {
			hueService.connectToBridgeIfNotAlreadyConnected(bridge);
		}
	}
	
}
