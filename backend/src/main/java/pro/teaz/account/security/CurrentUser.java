package pro.teaz.account.security;

import java.util.UUID;

/** Who is calling: the Keycloak user id (sub). From the JWT in prod, a fixed mock user otherwise. */
@FunctionalInterface
public interface CurrentUser {

	UUID id();
}
