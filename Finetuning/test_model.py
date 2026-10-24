import json
import ollama
 
 
MODEL_NAME = "my_model"
 
 
def load_inputs(file_path):
    with open(file_path, "r", encoding="utf-8") as file:
        return json.load(file)
 
 
def test_model():
    data = load_inputs("inputs.json")
 
    for test_case in data["test_cases"]:
        test_id = test_case["id"]
        question = test_case["question"]
 
        print("=" * 80)
        print(f"TEST CASE: {test_id}")
        print(f"INPUT: {question}")
        print("-" * 80)
 
        response = ollama.chat(
            model=MODEL_NAME,
            messages=[
                {
                    "role": "user",
                    "content": question
                }
            ]
        )
 
        answer = response["message"]["content"]
 
        print("OUTPUT:")
        print(answer)
        print()
 
 
if __name__ == "__main__":
    test_model()
 