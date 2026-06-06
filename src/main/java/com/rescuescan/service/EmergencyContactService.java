package com.rescuescan.service;

import com.rescuescan.model.EmergencyContact;
import com.rescuescan.model.User;
import com.rescuescan.repository.EmergencyContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class EmergencyContactService {

    private final EmergencyContactRepository contactRepository;

    public EmergencyContactService(EmergencyContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Transactional
    public void saveContacts(User user, List<String> names, List<String> phones, List<String> relationships) {
        contactRepository.deleteByUser(user);

        for (int i = 0; i < names.size(); i++) {
            if (names.get(i) != null && !names.get(i).trim().isEmpty()) {
                EmergencyContact contact = new EmergencyContact();
                contact.setUser(user);
                contact.setContactName(names.get(i).trim());
                contact.setContactPhone(phones.get(i).trim());
                contact.setRelationship(relationships.get(i).trim());
                contactRepository.save(contact);
            }
        }
    }

    public List<EmergencyContact> getContactsByUser(User user) {
        return contactRepository.findByUser(user);
    }
}
