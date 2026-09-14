package hackathon26.hackathon.note;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Note {

	private Long id;

	private String title;

	private String content;
}
