import asyncio
from typing import Any, Dict, List
import ollama

# 1. MCP Server Setup (Internal FastMCP instance)
from mcp.server.mcpserver import MCPServer

mcp_server = MCPServer("Local Tool Demo")


# Define a tool with robust input handling for small models
@mcp_server.tool()
def get_user_balance(user_id: str) -> str:
    """Retrieve the remaining account balance for a given user ID."""
    # Normalize input: if the model passed just '123', fix it to 'user_123'
    cleaned_id = user_id.strip()
    if not cleaned_id.startswith("user_"):
        cleaned_id = f"user_{cleaned_id}"

    balances = {
        "user_123": "$1,45777770.50 USD",
        "user_456": "$82.10 USD",
    }
    return balances.get(cleaned_id, f"User ID '{user_id}' not found.")


# 2. Conversion helper: Convert MCP Tool schemas into Ollama JSON format
async def get_ollama_tools() -> List[Dict[str, Any]]:
    mcp_tools = await mcp_server.list_tools()
    ollama_tools = []

    for tool in mcp_tools:
        ollama_tools.append(
            {
                "type": "function",
                "function": {
                    "name": tool.name,
                    "description": tool.description,
                    "parameters": tool.input_schema,
                },
            }
        )
    return ollama_tools


# 3. Execution loop: MCP Tool Orchestration with Ollama
async def run_agent(user_prompt: str, model_name: str = "my_model"):
    tools = await get_ollama_tools()

    messages = [{"role": "user", "content": user_prompt}]
    print(f"User Question: {user_prompt}\n" + "-" * 40)

    # First Turn: Pass prompt + MCP tools to Ollama
    response = ollama.chat(
        model=model_name,
        messages=messages,
        tools=tools,
    )

    messages.append(response.message)

    # Check if the model requested any MCP tool calls
    if response.message.tool_calls:
        for tool_call in response.message.tool_calls:
            tool_name = tool_call.function.name
            tool_args = tool_call.function.arguments

            print(f"🤖 Model selected tool: '{tool_name}' with args: {tool_args}")

            # Execute the tool via the MCP server engine
            try:
                mcp_result = await mcp_server.call_tool(tool_name, tool_args)
                tool_output = (
                    mcp_result[0].text
                    if isinstance(mcp_result, list)
                    else str(mcp_result)
                )
                print(f"🛠️ MCP Tool Output: {tool_output}\n")
            except Exception as e:
                tool_output = f"Error executing tool: {e}"

            # Return the tool output back to the model history
            messages.append(
                {
                    "role": "tool",
                    "content": tool_output,
                }
            )

        # Second Turn: Generate final answer with tool result in context
        final_response = ollama.chat(
            model=model_name,
            messages=messages,
        )
        print(f"Final Answer:\n{final_response.message.content}")

    else:
        print(f"Final Answer:\n{response.message.content}")


if __name__ == "__main__":
    prompt = "What is the account balance for user_123?"
    asyncio.run(run_agent(prompt, model_name="my_model"))