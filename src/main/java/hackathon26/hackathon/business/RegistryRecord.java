package hackathon26.hackathon.business;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "registry_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistryRecord {
    @Id
    private String registryNumber;
    private String parentRegistryNumber;
    private String recordType;
    private String officialName;
    private String tradeName;
    private String searchName;
    private String legalStatus;
    private String legalForm;
    private String kboStreet;
    private String kboHouseNumber;
    private String kboBusNumber;
    private String kboPostcode;
    private String municipality;
    private String addressStreet;
    private String addressHouseNumber;
    private String addressBusNumber;
    private String addressPostcode;
    private String activityDescription;
    private String registrationDate;
    private String startDate;
    private String cessationDate;
    private String latitude;
    private String longitude;
    private String phone;
    private String email;
    private String annualAccountUrl;
}
