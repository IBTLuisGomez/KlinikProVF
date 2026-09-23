package systems.cytohelix.klinikpro_vf.agenda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByBranchIdOrderByNameAsc(UUID branchId);
    Optional<Room> findByIdAndBranchId(UUID id, UUID branchId);
}
