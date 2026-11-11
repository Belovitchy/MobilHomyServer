package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.*;
import fr.eric.mobilhomy.dto.AdminOwnerRequestDto;
import fr.eric.mobilhomy.dto.AdminOwnerResponseDto;
import fr.eric.mobilhomy.exception.BusinessRuleException;
import fr.eric.mobilhomy.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Gestion des comptes propriétaires par l'administrateur. */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private OwnerRepository ownerRepository;
    @Mock private AuthRepository authRepository;
    @Mock private MobilhomeRepository mobilhomeRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    private Owner ownerWithAccount(boolean enabled) {
        Owner o = Owner.builder().id(2).firstname("Paul").name("Dupont").build();
        o.setAuth(Auth.builder().login("dupont@mail.fr").authority(RolesEnum.OWNER).enabled(enabled).utilisateur(o).build());
        return o;
    }

    @Test
    void suspendre_desactiveLeCompte() {
        Owner o = ownerWithAccount(true);
        when(ownerRepository.findById(2)).thenReturn(Optional.of(o));

        AdminOwnerResponseDto res = adminService.setOwnerEnabled(2, false);

        assertFalse(o.getAuth().isEnabled());
        assertFalse(res.enabled());
        verify(authRepository).save(o.getAuth());
    }

    @Test
    void reactiver_reactiveLeCompte() {
        Owner o = ownerWithAccount(false);
        when(ownerRepository.findById(2)).thenReturn(Optional.of(o));

        assertTrue(adminService.setOwnerEnabled(2, true).enabled());
    }

    @Test
    void suspendre_unProprietaireInconnu_renvoieNotFound() {
        when(ownerRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> adminService.setOwnerEnabled(99, false));
        verify(authRepository, never()).save(any());
    }

    @Test
    void creerUnProprietaire_emailDejaUtilise_refuse() {
        when(authRepository.existsByLogin("dupont@mail.fr")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> adminService.createOwner(
                new AdminOwnerRequestDto("Paul", "Dupont", " Dupont@Mail.fr ", "", "motdepasse8")));
        verify(authRepository, never()).save(any());
    }

    @Test
    void creerUnProprietaire_hacheLeMotDePasseEtDonneLeRoleOwner() {
        when(authRepository.existsByLogin("roy@mail.fr")).thenReturn(false);
        when(passwordEncoder.encode("motdepasse8")).thenReturn("{bcrypt}hash");

        AdminOwnerResponseDto res = adminService.createOwner(
                new AdminOwnerRequestDto("Anne", "Roy", "roy@mail.fr", "", "motdepasse8"));

        verify(authRepository).save(argThat(a -> a.getAuthority() == RolesEnum.OWNER
                && "{bcrypt}hash".equals(a.getPassword())
                && a.isEnabled()
                && a.getUtilisateur() instanceof Owner));
        assertEquals("roy@mail.fr", res.email());
        assertNull(res.phone());
    }
}
