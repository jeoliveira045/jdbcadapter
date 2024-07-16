package labs.example;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Pessoa {

    private Long id;

    private String nome;

    private String cpf;

    private String endereco;

    private LocalDate dataNascimento;

}
