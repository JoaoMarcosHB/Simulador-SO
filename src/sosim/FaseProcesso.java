package sosim;

// fases que um processo de usuario pode passar
// CPU_BOUND e quando nao tem fase de I/O (cpu2 = 0)
public enum FaseProcesso {
    CPU1,
    IO,
    CPU2,
    CONCLUIDO
}
