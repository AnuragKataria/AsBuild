
import sys

def count_braces(file_path):
    with open(file_path, 'r') as f:
        content = f.read()
    
    open_braces = 0
    close_braces = 0
    for i, char in enumerate(content):
        if char == '{':
            open_braces += 1
        elif char == '}':
            close_braces += 1
        
        if close_braces > open_braces:
            print(f"Extra closing brace at position {i}")
            # print surrounding text
            start = max(0, i - 50)
            end = min(len(content), i + 50)
            print(f"Context: {content[start:end]}")
            return
    
    print(f"Open: {open_braces}, Close: {close_braces}")

if __name__ == "__main__":
    count_braces(sys.argv[1])
