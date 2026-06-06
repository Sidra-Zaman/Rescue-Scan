package com.rescuescan.controller;

import com.rescuescan.model.EmergencyContact;
import com.rescuescan.model.User;
import com.rescuescan.service.EmergencyContactService;
import com.rescuescan.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/qr/public")
public class PublicQrController {

    private final UserService userService;
    private final EmergencyContactService contactService;

    public PublicQrController(UserService userService, EmergencyContactService contactService) {
        this.userService = userService;
        this.contactService = contactService;
    }

    @GetMapping("/{qrCode}")
    public String viewEmergencyInfo(@PathVariable String qrCode, Model model) {
        User user = userService.findByQrCode(qrCode);
        List<EmergencyContact> contacts = contactService.getContactsByUser(user);

        model.addAttribute("user", user);
        model.addAttribute("contacts", contacts);
        return "public-info";
    }
}
