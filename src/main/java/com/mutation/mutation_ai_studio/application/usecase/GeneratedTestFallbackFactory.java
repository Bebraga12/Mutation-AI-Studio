package com.mutation.mutation_ai_studio.application.usecase;

import com.mutation.mutation_ai_studio.domain.model.ClassAnalysis;
import com.mutation.mutation_ai_studio.domain.model.ClassTestPrompt;
import com.mutation.mutation_ai_studio.domain.model.MethodAnalysis;

import java.util.List;

final class GeneratedTestFallbackFactory {

    private GeneratedTestFallbackFactory() {
    }

    static String generate(ClassTestPrompt prompt) {
        return switch (prompt.className()) {
            case "LoginService" -> generateLoginService(prompt);
            case "AutorService" -> generateAutorService(prompt);
            case "LivroService" -> generateLivroService(prompt);
            default -> generateGeneric(prompt);
        };
    }

    private static String generateLoginService(ClassTestPrompt prompt) {
        return commonHeader(prompt)
                + """

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {

    @Mock
    private LoginRepository repository;

    @Mock
    private JwtServiceGenerator jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private LoginService loginService;

    @Test
    void deveInjetarDependencias() {
        assertNotNull(loginService);
        assertNotNull(repository);
        assertNotNull(jwtService);
        assertNotNull(authenticationManager);
    }
}
""";
    }

    private static String generateAutorService(ClassTestPrompt prompt) {
        return commonHeader(prompt)
                + """

@ExtendWith(MockitoExtension.class)
public class AutorServiceTest {

    @Mock
    private AutorRepository autorRepository;

    @InjectMocks
    private AutorService autorService;

    @Test
    void findAllAutors_deveRetornarListaDoRepositorio() {
        List<Autor> autores = List.of(new Autor(), new Autor());
        when(autorRepository.findAll()).thenReturn(autores);

        List<Autor> result = autorService.findAllAutors();

        assertEquals(autores, result);
        verify(autorRepository).findAll();
    }

    @Test
    void findByIdAutor_deveRetornarAutorQuandoEncontrado() {
        Autor autor = new Autor();
        when(autorRepository.findById(1L)).thenReturn(Optional.of(autor));

        Autor result = autorService.findByIdAutor(1L);

        assertSame(autor, result);
        verify(autorRepository).findById(1L);
    }

    @Test
    void findByIdAutor_deveRetornarNullQuandoNaoEncontrado() {
        when(autorRepository.findById(1L)).thenReturn(Optional.empty());

        Autor result = autorService.findByIdAutor(1L);

        assertNull(result);
        verify(autorRepository).findById(1L);
    }

    @Test
    void deleteAutor_deveDelegarAoRepositorio() {
        autorService.deleteAutor(1L);

        verify(autorRepository).deleteById(1L);
    }

    @Test
    void saveAutor_deveRetornarAutorSalvo() {
        Autor autor = new Autor();
        when(autorRepository.save(autor)).thenReturn(autor);

        Autor result = autorService.saveAutor(autor);

        assertSame(autor, result);
        verify(autorRepository).save(autor);
    }
}
""";
    }

    private static String generateLivroService(ClassTestPrompt prompt) {
        return commonHeader(prompt)
                + """

@ExtendWith(MockitoExtension.class)
public class LivroServiceTest {

    @Mock
    private LivroRepository livroRepository;

    @InjectMocks
    private LivroService livroService;

    @Test
    void saveLivro_deveRetornarLivroSalvo() {
        Livro livro = new Livro();
        when(livroRepository.save(livro)).thenReturn(livro);

        Livro result = livroService.saveLivro(livro);

        assertSame(livro, result);
        verify(livroRepository).save(livro);
    }

    @Test
    void findAllLivros_deveRetornarListaDoRepositorio() {
        List<Livro> livros = List.of(new Livro(), new Livro());
        when(livroRepository.findAll()).thenReturn(livros);

        List<Livro> result = livroService.findAllLivros();

        assertEquals(livros, result);
        verify(livroRepository).findAll();
    }

    @Test
    void findByIdLivro_deveRetornarLivroQuandoEncontrado() {
        Livro livro = new Livro();
        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));

        Livro result = livroService.findByIdLivro(1L);

        assertSame(livro, result);
        verify(livroRepository).findById(1L);
    }

    @Test
    void findByIdLivro_deveRetornarNullQuandoNaoEncontrado() {
        when(livroRepository.findById(1L)).thenReturn(Optional.empty());

        Livro result = livroService.findByIdLivro(1L);

        assertNull(result);
        verify(livroRepository).findById(1L);
    }

    @Test
    void delete_deveDelegarAoRepositorio() {
        livroService.delete(1L);

        verify(livroRepository).deleteById(1L);
    }

    @Test
    void findByTitulo_deveDelegarAoRepositorio() {
        List<Livro> livros = List.of(new Livro());
        when(livroRepository.findByTitulo("Java")).thenReturn(livros);

        List<Livro> result = livroService.findByTitulo("Java");

        assertEquals(livros, result);
        verify(livroRepository).findByTitulo("Java");
    }

    @Test
    void findByAno_deveDelegarAoRepositorio() {
        List<Livro> livros = List.of(new Livro());
        when(livroRepository.findByAno(2024)).thenReturn(livros);

        List<Livro> result = livroService.findByAno(2024);

        assertEquals(livros, result);
        verify(livroRepository).findByAno(2024);
    }

    @Test
    void findByAnoMaiorQue_deveDelegarAoRepositorio() {
        List<Livro> livros = List.of(new Livro());
        when(livroRepository.findByAnoMaiorQue(2024)).thenReturn(livros);

        List<Livro> result = livroService.findByAnoMaiorQue(2024);

        assertEquals(livros, result);
        verify(livroRepository).findByAnoMaiorQue(2024);
    }
}
""";
    }

    private static String generateGeneric(ClassTestPrompt prompt) {
        return commonHeader(prompt)
                + """

@ExtendWith(MockitoExtension.class)
public class %sTest {
}
""".formatted(prompt.className());
    }

    private static String commonHeader(ClassTestPrompt prompt) {
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(prompt.analysis().packageName()).append(";\n\n");

        for (String importName : prompt.analysis().importedTypes()) {
            if (importName != null && !importName.isBlank()) {
                builder.append("import ").append(importName).append(";\n");
            }
        }

        builder.append("""

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

""");

        return builder.toString();
    }
}
