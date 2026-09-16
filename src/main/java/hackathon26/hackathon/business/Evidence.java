package hackathon26.hackathon.business;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "business_evidence")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evidence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String registryNumber;
    private String sourceType;
    private String sourceUrl;
    private String observation;
    private LocalDate observedOn;
    private OffsetDateTime capturedAt;
    private String officer;
}
