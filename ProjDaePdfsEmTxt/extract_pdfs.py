import os
from pypdf import PdfReader

pdf_dir = r"c:\Users\tomas\OneDrive\Ambiente de Trabalho\DAE\dae_docs_proj_pratico"
output_dir = "pdf_texts"

os.makedirs(output_dir, exist_ok=True)

pdf_files = [
    "DAE-2025-26-1S-ENUNCIADO_PROJETO.pdf",
    "EI_DAE_2025_26_Ficha1.pdf",
    "EI_DAE_2025-26_Ficha2.pdf",
    "EI_DAE_2025_2026_Ficha3.pdf",
    "EI_DAE_2025_2026_Ficha4.pdf",
    "EI_DAE_2025_2026_Ficha5.pdf",
    "EI_DAE_2025_2026_FICHA6_v2.pdf",
    "EI_DAE_2025_2026_FICHA7_v3.pdf",
    "EI_DAE_2025_2026_FICHA8.pdf",
    "EI_DAE_2025_2026_FICHA9.pdf"
]

for pdf_file in pdf_files:
    pdf_path = os.path.join(pdf_dir, pdf_file)
    if os.path.exists(pdf_path):
        try:
            reader = PdfReader(pdf_path)
            text_content = []
            for page in reader.pages:
                text_content.append(page.extract_text())
            
            output_file = os.path.join(output_dir, pdf_file.replace(".pdf", ".txt"))
            with open(output_file, "w", encoding="utf-8") as f:
                f.write("\n".join(text_content))
            
            print(f"Extraído: {pdf_file} ({len(reader.pages)} páginas)")
        except Exception as e:
            print(f"Erro ao processar {pdf_file}: {e}")
    else:
        print(f"Ficheiro não encontrado: {pdf_path}")

print("\nExtração concluída!")


