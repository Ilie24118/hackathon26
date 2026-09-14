package hackathon26.hackathon.note;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

	private final NoteRepository noteRepository;

	public NoteController(NoteRepository noteRepository) {
		this.noteRepository = noteRepository;
	}

	@GetMapping
	public List<Note> list() {
		return noteRepository.findAll();
	}

	@GetMapping("/{id}")
	public Note get(@PathVariable Long id) {
		return noteRepository.findById(id)
				.orElseThrow(() -> notFound(id));
	}

	@PostMapping
	public ResponseEntity<Note> create(@RequestBody Note note) {
		Note saved = noteRepository.save(note);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@PutMapping("/{id}")
	public Note update(@PathVariable Long id, @RequestBody Note note) {
		if (!noteRepository.update(id, note)) {
			throw notFound(id);
		}
		note.setId(id);
		return note;
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		if (!noteRepository.deleteById(id)) {
			throw notFound(id);
		}
		return ResponseEntity.noContent().build();
	}

	private ResponseStatusException notFound(Long id) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found: " + id);
	}
}
