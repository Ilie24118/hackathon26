package hackathon26.hackathon.business;

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
@Table(name = "review_decisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String registryNumber;
    private String status;
    private String note;
    private String officer;
    private OffsetDateTime decidedAt;
}
