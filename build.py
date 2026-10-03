import os
import sys
import subprocess
import shutil
import zipfile

base_dir = os.path.dirname(os.path.abspath(__file__))

# Find JDK 21
javac_path = r'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\javac.exe'
jar_path = r'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot\bin\jar.exe'

if not os.path.exists(javac_path):
    javac_path = 'javac'
    jar_path = 'jar'

def build():
    print("==================================================")
    print("  BUILDING: ZOLTRAAK CINEMATIC EDITION (NEOFORGE 1.21.1)")
    print("==================================================")

    out_jar = os.path.join(base_dir, 'zoltraak_cinematic-neoforge-1.21.1-1.0.0.jar')
    parent_jar = os.path.join(base_dir, '..', 'zoltraak_cinematic-neoforge-1.21.1-1.0.0.jar')
    bin_dir = os.path.join(base_dir, 'build', 'classes')
    nf_client = os.path.join(base_dir, 'libs', 'neoforge-21.1.248-client.jar')
    nf_univ = os.path.join(base_dir, 'libs', 'neoforge-21.1.248-universal.jar')
    libs_cp = f"{nf_client}{os.pathsep}{nf_univ}{os.pathsep}" + os.path.join(base_dir, 'libs', '*')
    src_dir = os.path.join(base_dir, 'src', 'main', 'java')
    res_dir = os.path.join(base_dir, 'src', 'main', 'resources')

    if os.path.exists(bin_dir):
        shutil.rmtree(bin_dir)
    os.makedirs(bin_dir, exist_ok=True)

    # 1. Collect Java files
    java_files = []
    for root, _, files in os.walk(src_dir):
        for f in files:
            if f.endswith('.java'):
                java_files.append(os.path.join(root, f))

    print(f"Compiling {len(java_files)} Java files with Java 21...")
    cmd = [javac_path, '-source', '21', '-target', '21', '-proc:none', '-cp', libs_cp, '-d', bin_dir] + java_files
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print("[ERROR] Compilation failed!")
        print(res.stderr)
        sys.exit(1)
    print("[SUCCESS] Compilation completed!")

    # 2. Copy resources
    print("Copying resources...")
    for root, _, files in os.walk(res_dir):
        rel = os.path.relpath(root, res_dir)
        target_root = os.path.join(bin_dir, rel) if rel != '.' else bin_dir
        os.makedirs(target_root, exist_ok=True)
        for f in files:
            src = os.path.join(root, f)
            dst = os.path.join(target_root, f)
            shutil.copy2(src, dst)

    # 3. Create JAR archive
    print(f"Building JAR: {out_jar}")
    cmd_jar = [jar_path, '--create', '--file', out_jar, '-C', bin_dir, '.']
    res_jar = subprocess.run(cmd_jar, capture_output=True, text=True)
    if res_jar.returncode != 0:
        print("[ERROR] JAR creation failed!")
        print(res_jar.stderr)
        sys.exit(1)

    print(f"[SUCCESS] Created: {out_jar} ({os.path.getsize(out_jar)} bytes)")

    # Copy to parent mod directory
    try:
        shutil.copy2(out_jar, parent_jar)
        print(f"[SUCCESS] Copied to parent folder: {parent_jar}")
    except Exception as e:
        print(f"[WARNING] Could not copy to parent: {e}")

    # Copy to Curseforge test instances
    target_instances = [
        r"C:\Users\vivo9\curseforge\minecraft\Instances\LING Horizons2.0test\mods",
        r"C:\Users\vivo9\curseforge\minecraft\Instances\G&D (1)\mods"
    ]
    for target_dir in target_instances:
        if os.path.exists(target_dir):
            try:
                dest = os.path.join(target_dir, os.path.basename(out_jar))
                shutil.copy2(out_jar, dest)
                print(f"[SUCCESS] Copied to instance: {dest}")
                # Also overwrite beta2 filename if it exists in target_dir
                beta2 = os.path.join(target_dir, "zoltraak_cinematic-neoforge-1.21.1-1.0.0beta2.jar")
                if os.path.exists(beta2):
                    shutil.copy2(out_jar, beta2)
                    print(f"[SUCCESS] Updated beta2 file in instance: {beta2}")
            except Exception as e:
                print(f"[WARNING] Could not copy to instance {target_dir}: {e}")

    # 4. Verify contents
    with zipfile.ZipFile(out_jar, 'r') as z:
        print(f"Total items in JAR: {len(z.namelist())}")
        print("\n--- Key JAR Entries ---")
        for item in sorted(z.namelist()):
            if any(k in item for k in ['Zoltraak', 'ModCinematic', 'frieren_staff', 'zoltraak_vfx_atlas', 'neoforge.mods.toml']):
                print(" ", item)

    print("\n==================================================")
    print("  BUILD COMPLETE: Zoltraak Cinematic Edition is ready to play!")
    print("==================================================")

if __name__ == '__main__':
    build()
