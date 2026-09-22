package systems.cytohelix.klinikpro_vf.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}