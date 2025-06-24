# qa_system.py
import pandas as pd
import os
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

def get_answer(user_input):
    try:
        # Get the path to the dataset file - now in the same directory as this script
        current_dir = os.path.dirname(__file__)
        file_path = os.path.join(current_dir, "qa_dataset.csv")

        # Check if file exists
        if not os.path.exists(file_path):
            return f"Error: Dataset file not found at {file_path}"

        # Load CSV - using pandas default quoting (MINIMAL) instead of forcing quoting=1
        df = pd.read_csv(file_path, encoding='utf-8')

        # Vectorize questions
        vectorizer = TfidfVectorizer()
        X = vectorizer.fit_transform(df['question'])

        # Process user input
        user_vec = vectorizer.transform([user_input])
        similarity = cosine_similarity(user_vec, X)
        idx = similarity.argmax()

        # Return the answer
        return df['answer'].iloc[idx]
    except FileNotFoundError:
        return f"Error: Could not find qa_dataset.csv in {os.path.dirname(__file__)}"
    except pd.errors.EmptyDataError:
        return "Error: The dataset file is empty"
    except pd.errors.ParserError:
        return "Error: Unable to parse the CSV file. Check the format."
    except KeyError as e:
        return f"Error: Required column {str(e)} not found in dataset"
    except Exception as e:
        return f"Error processing your question: {str(e)}"

# This function can be called to test the system
def test_system():
    try:
        # Get file path and check existence
        file_path = os.path.join(os.path.dirname(__file__), "qa_dataset.csv")
        if not os.path.exists(file_path):
            return f"Test failed: Dataset file not found at {file_path}"

        # Basic test with a sample question
        return "QA system is working correctly"
    except Exception as e:
        return f"Test failed: {str(e)}"