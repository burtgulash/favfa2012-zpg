\ProvidesClass{zcu_sp}

\LoadClass[12pt,a4paper]{article}

\usepackage[utf8]{inputenc}
\usepackage[czech]{babel}
\usepackage{epsf}
\usepackage{graphicx}
\usepackage{xcolor}
% \usepackage{czech, epsf, graphicx, xcolor}

\renewcommand\refname{Literatura}

\makeatletter
	\newcommand{\course}[1]{\def \@course {#1}}
	\newcommand{\email}[1]{\def \@email {#1}}
	\course{}
	\email{}
\makeatother
\addtolength{\hoffset}{-1in}
\addtolength{\textwidth}{2cm}
\addtolength{\evensidemargin}{1.5cm}
\addtolength{\oddsidemargin}{1.5cm}
\addtolength{\voffset}{-1in}
\addtolength{\textheight}{2cm}
\addtolength{\topmargin}{1.5cm}
\addtolength{\headheight}{.3cm}


\font\fntt = cmsltt10 scaled 1200
\font\ssbfl = csssbx10 scaled 1440
\font\ssbf = csssbx10 scaled 1200

\makeatletter

\renewcommand\section{\@startsection {section}{1}{\z@}%
                                   {-3.5ex \@plus -1ex \@minus -.2ex}%
                                   {2.3ex \@plus.2ex}%
                                   {\ssbfl}}
\renewcommand\subsection{\@startsection{subsection}{2}{\z@}%
                                     {-3.25ex\@plus -1ex \@minus -.2ex}%
                                     {1.5ex \@plus .2ex}%
                                     {\tiny\ssbf}}
\renewcommand\subsubsection{\@startsection{subsubsection}{3}{\z@}%
                                     {-3.25ex\@plus -1ex \@minus -.2ex}%
                                     {1.5ex \@plus .2ex}%
                                     {\ssbf}}


\usepackage[hang,small,bf]{caption}

\makeatother
\makeatletter
	\usepackage{fancyhdr}
	\pagestyle{fancy}
	\renewcommand{\footrulewidth}{\headrulewidth}
	\fancyhf{}

	\newcommand{\updateheaders}{		
		\ifx \@author \@empty
			\relax
		\else
			\fancyhead[R]{\ssbf\@author}
		\fi

		\ifx \@course \@empty
			\relax
		\else
			\fancyhead[L]{\ssbf\@course}
		\fi
	}

	\fancyfoot[R]{\ssbf\thepage}
\makeatother


\makeatletter
\usepackage{listings}

\lstset{
        tabsize=2,
        basicstyle=\ttfamily\footnotesize,
        keywordstyle=\color{blue},
        keywordstyle=[2]\color{teal},
        commentstyle=\color{gray},
        stringstyle=\color{green},
        showstringspaces=false,
        captionpos=b
}
% \renewcommand\lstlistingname{Kód}
\makeatother


\makeatletter
\def \maketitle {%
	% \changepage{3cm}{2cm}{-1cm}{-1cm}{}{-1cm}{}{}{}
	\begin{titlepage}
	        \begin{center}
			\hbox to \textwidth{\epsfysize=32mm\raise 6.3mm\hbox{\includegraphics[height=32mm]{logo_zcu}}\hfill}

			\vskip 5cm\hrule\vskip 2.2ex
				{\LARGE\sf Semestrální práce \@course\\}
				{\Large\sf \@title\\}
			\vskip 1.7ex\hrule
			\vskip 2ex
		\end{center}

		\vfill
		\begin{flushright}
			{\bf \@author\\}
			\@email\\
			\@date\\
		\end{flushright}
	\end{titlepage}
	%\changepage{-3cm}{-2cm}{1cm}{1cm}{}{1cm}{}{}{}


Souhlasím s vystavením této semestrální práce na stránkách katedry informatiky a~výpočetní techniky a~jejímu využití pro prezentaci pracoviště.
	\vspace{1cm}

	\hfill\@author
	
	\newpage
	\updateheaders
}
\makeatother
