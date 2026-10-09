package org.kt.dao;

import java.util.Optional;
import org.kt.exceptions.DBException;
import org.kt.model.Usuario;

public interface UsuarioDAO extends CrudDAO<Usuario> {

    Optional<Usuario> buscarPorUsername(String username)
            throws DBException;
}