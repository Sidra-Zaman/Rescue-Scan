package com.rescuescan.controller;

import com.google.zxing.WriterException;
import com.rescuescan.model.EmergencyContact;
import com.rescuescan.model.User;
import com.rescuescan.repository.UserRepository;
import com.rescuescan.service.EmergencyContactService;
import com.rescuescan.service.QrCodeService;
import com.rescuescan.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final UserService userService;
    private final EmergencyContactService contactService;
    private final QrCodeService qrCodeService;
    private final UserRepository userRepository;

    public DashboardController(UserService userService,
                               EmergencyContactService contactService,
                               QrCodeService qrCodeService,
                               UserRepository userRepository) {
        this.userService = userService;
        this.contactService = contactService;
        this.qrCodeService = qrCodeService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());

        if (!user.isProfileCompleted()) {
            return "redirect:/dashboard/setup";
        }

        model.addAttribute("user", user);
        model.addAttribute("contacts", contactService.getContactsByUser(user));
        model.addAttribute("qrCodeUrl", qrCodeService.getQrCodeUrl(user.getQrCode()));
        return "dashboard";
    }

    @GetMapping("/setup")
    public String setupForm(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("contacts", contactService.getContactsByUser(user));
        return "setup";
    }

    @PostMapping("/setup/preview")
    public String setupPreview(Authentication authentication,
                               @RequestParam List<String> contactName,
                               @RequestParam List<String> contactPhone,
                               @RequestParam List<String> relationship,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());

        boolean hasValidContact = false;
        for (String name : contactName) {
            if (name != null && !name.trim().isEmpty()) {
                hasValidContact = true;
                break;
            }
        }

        if (!hasValidContact) {
            redirectAttributes.addFlashAttribute("error", "Please add at least one emergency contact");
            return "redirect:/dashboard/setup";
        }

        List<EmergencyContact> previewContacts = new ArrayList<>();
        for (int i = 0; i < contactName.size(); i++) {
            if (contactName.get(i) != null && !contactName.get(i).trim().isEmpty()) {
                EmergencyContact contact = new EmergencyContact();
                contact.setContactName(contactName.get(i).trim());
                contact.setContactPhone(contactPhone.get(i).trim());
                contact.setRelationship(relationship.get(i).trim());
                previewContacts.add(contact);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("contacts", previewContacts);
        return "confirm-setup";
    }

    @PostMapping("/setup/confirm")
    public String setupConfirm(Authentication authentication,
                               @RequestParam List<String> contactName,
                               @RequestParam List<String> contactPhone,
                               @RequestParam List<String> relationship,
                               RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());

        contactService.saveContacts(user, contactName, contactPhone, relationship);
        userService.completeProfile(user);

        redirectAttributes.addFlashAttribute("success", "Profile setup complete!");
        return "redirect:/dashboard/qr";
    }

    @GetMapping("/qr")
    public String viewQrCode(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        List<EmergencyContact> contacts = contactService.getContactsByUser(user);
        model.addAttribute("user", user);
        model.addAttribute("contacts", contacts);
        model.addAttribute("qrCodeUrl", qrCodeService.getQrCodeUrl(user.getQrCode()));
        model.addAttribute("offlineQrCodeUrl", "/dashboard/qr/offline-image");
        model.addAttribute("offlineText", buildEmergencyInfoText(user, contacts));
        return "qr-code";
    }

    @GetMapping("/qr/image")
    @ResponseBody
    public byte[] getQrImage(Authentication authentication) throws WriterException, IOException {
        User user = userService.findByUsername(authentication.getName());
        String url = "http://localhost:8080/qr/public/" + user.getQrCode();
        return qrCodeService.generateQrCode(url);
    }

    @GetMapping("/qr/offline-image")
    @ResponseBody
    public byte[] getOfflineQrImage(Authentication authentication) throws WriterException, IOException {
        User user = userService.findByUsername(authentication.getName());
        List<EmergencyContact> contacts = contactService.getContactsByUser(user);
        String offlineContent = buildEmergencyInfoText(user, contacts);
        return qrCodeService.generateQrCode(offlineContent);
    }

    private String buildEmergencyInfoText(User user, List<EmergencyContact> contacts) {
        StringBuilder sb = new StringBuilder();
        sb.append("EMERGENCY: ").append(user.getUsername()).append("\n");
        sb.append("---------------------------\n");
        int count = 1;
        for (EmergencyContact contact : contacts) {
            if (contact.getContactName() != null && !contact.getContactName().isEmpty()) {
                sb.append(count).append(". ").append(contact.getContactName())
                  .append(" | ").append(contact.getContactPhone());
                if (contact.getRelationship() != null && !contact.getRelationship().isEmpty()) {
                    sb.append(" | ").append(contact.getRelationship());
                }
                sb.append("\n");
                count++;
            }
        }
        return sb.toString();
    }

    @GetMapping("/contacts")
    public String manageContacts(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("contacts", contactService.getContactsByUser(user));
        return "contacts";
    }

    @PostMapping("/contacts/update")
    public String updateContacts(Authentication authentication,
                                @RequestParam List<String> contactName,
                                @RequestParam List<String> contactPhone,
                                @RequestParam List<String> relationship,
                                RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());
        contactService.saveContacts(user, contactName, contactPhone, relationship);
        redirectAttributes.addFlashAttribute("success", "Contacts updated successfully!");
        return "redirect:/dashboard/contacts";
    }
}
