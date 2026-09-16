package hackathon26.hackathon.business;

import java.time.OffsetDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "verified_businesses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifiedBusiness {
    @Id
    private String registryNumber;
    private String verifiedStatus;
    private String note;
    private String officer;
    private OffsetDateTime verifiedAt;
}
