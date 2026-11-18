/**
 * Conteúdo mockado da landing page para demonstração.
 * Substituir por depoimentos e respostas reais antes de publicar.
 */
export type Depoimento = {
  citacao: string
  assinatura: string
}

export type PerguntaFrequente = {
  pergunta: string
  resposta: string
}

export const depoimentoMock: Depoimento = {
  citacao:
    'A OS saiu do caderno. Hoje eu abro o painel e sei o que está pronto, o que falta e quanto vai entrar no caixa.',
  assinatura: 'Rafael Menezes, Oficina do Rafael, Campinas',
}

export const perguntasFrequentesMock: PerguntaFrequente[] = [
  {
    pergunta: 'Preciso instalar alguma coisa?',
    resposta: 'Não. O MotoServices roda no navegador — basta entrar com usuário e senha.',
  },
  {
    pergunta: 'Funciona no celular?',
    resposta: 'Sim. O painel se adapta ao celular e ao tablet, com as mesmas informações.',
  },
  {
    pergunta: 'Posso importar meus clientes e motos?',
    resposta:
      'A importação assistida faz parte do piloto; a equipe avalia a planilha que você já usa.',
  },
  {
    pergunta: 'Quanto custa?',
    resposta:
      'As condições são definidas por proposta, conforme o tamanho e a rotina da oficina.',
  },
  {
    pergunta: 'Meus dados ficam seguros?',
    resposta:
      'O acesso é individual e por perfil; cada pessoa vê apenas o que precisa para o seu papel.',
  },
]
